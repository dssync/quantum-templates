using System.IO;
using System.Text.RegularExpressions;
using UnityEditor;
using UnityEditor.Callbacks;
using UnityEditorInternal;
using UnityEngine;

namespace Quantum {

    public static class QtnScriptRedirect {

        const string PrototypePrefix = "QPrototype";

        [OnOpenAsset(0)]
        static bool OnOpenAsset(int instanceId, int line) {
            if (Event.current != null && Event.current.shift) return false;
            if (EditorUtility.InstanceIDToObject(instanceId) is not MonoScript script) return false;

            var type = script.GetClass();
            if (type == null || !typeof(QuantumUnityComponentPrototype).IsAssignableFrom(type)) return false;
            if (!type.Name.StartsWith(PrototypePrefix)) return false;

            var componentName = type.Name.Substring(PrototypePrefix.Length);
            if (!TryFindDeclaration(componentName, out var path, out var declLine)) return false;

            return InternalEditorUtility.OpenFileAtLineExternal(path, declLine);
        }

        static bool TryFindDeclaration(string componentName, out string path, out int line) {
            var pattern = new Regex($@"^\s*(singleton\s+)?component\s+{Regex.Escape(componentName)}\b");
            foreach (var file in Directory.EnumerateFiles(Application.dataPath, "*.qtn", SearchOption.AllDirectories)) {
                var lines = File.ReadAllLines(file);
                for (int i = 0; i < lines.Length; i++) {
                    if (!pattern.IsMatch(lines[i])) continue;
                    path = file;
                    line = i + 1;
                    return true;
                }
            }
            path = null;
            line = 0;
            return false;
        }
    }
}
