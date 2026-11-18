function Set-DreamHouseLauncherUtf8([Parameter(Mandatory=$true)][string]$Executable) {
    if (!('DreamHouseLauncherResources' -as [type])) {
        Add-Type -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Runtime.InteropServices;

public static class DreamHouseLauncherResources {
    private static readonly IntPtr Manifest = (IntPtr)24;
    private static readonly IntPtr ProcessManifest = (IntPtr)1;
    private delegate bool LanguageCallback(IntPtr module, IntPtr type, IntPtr name, ushort language, IntPtr context);

    [DllImport("kernel32.dll", CharSet=CharSet.Unicode, SetLastError=true)]
    private static extern IntPtr LoadLibraryEx(string file, IntPtr handle, uint flags);
    [DllImport("kernel32.dll")] private static extern bool FreeLibrary(IntPtr module);
    [DllImport("kernel32.dll", CharSet=CharSet.Unicode, SetLastError=true)]
    private static extern bool EnumResourceLanguages(IntPtr module, IntPtr type, IntPtr name, LanguageCallback callback, IntPtr context);
    [DllImport("kernel32.dll", CharSet=CharSet.Unicode, SetLastError=true)]
    private static extern IntPtr FindResourceEx(IntPtr module, IntPtr type, IntPtr name, ushort language);
    [DllImport("kernel32.dll", SetLastError=true)] private static extern uint SizeofResource(IntPtr module, IntPtr resource);
    [DllImport("kernel32.dll", SetLastError=true)] private static extern IntPtr LoadResource(IntPtr module, IntPtr resource);
    [DllImport("kernel32.dll")] private static extern IntPtr LockResource(IntPtr resource);
    [DllImport("kernel32.dll", CharSet=CharSet.Unicode, SetLastError=true)]
    private static extern IntPtr BeginUpdateResource(string file, bool deleteExisting);
    [DllImport("kernel32.dll", CharSet=CharSet.Unicode, SetLastError=true)]
    private static extern bool UpdateResource(IntPtr update, IntPtr type, IntPtr name, ushort language, byte[] data, uint size);
    [DllImport("kernel32.dll", CharSet=CharSet.Unicode, SetLastError=true)]
    private static extern bool EndUpdateResource(IntPtr update, bool discard);

    public static Dictionary<ushort, byte[]> Read(string path) {
        var module = LoadLibraryEx(path, IntPtr.Zero, 2); // Map data without executing the image.
        if (module == IntPtr.Zero) throw new Win32Exception();
        try {
            var languages = new List<ushort>();
            bool found = EnumResourceLanguages(module, Manifest, ProcessManifest,
                (image, type, name, language, context) => { languages.Add(language); return true; }, IntPtr.Zero);
            int error = Marshal.GetLastWin32Error();
            if (!found && error != 1813 && error != 1814) throw new Win32Exception(error);
            var manifests = new Dictionary<ushort, byte[]>();
            foreach (ushort language in languages) {
                var resource = FindResourceEx(module, Manifest, ProcessManifest, language);
                if (resource == IntPtr.Zero) throw new Win32Exception();
                var data = new byte[SizeofResource(module, resource)];
                var handle = LoadResource(module, resource);
                if (handle == IntPtr.Zero) throw new Win32Exception();
                Marshal.Copy(LockResource(handle), data, 0, data.Length);
                manifests.Add(language, data);
            }
            return manifests;
        } finally { FreeLibrary(module); }
    }

    public static void Write(string path, Dictionary<ushort, byte[]> manifests) {
        var update = BeginUpdateResource(path, false);
        if (update == IntPtr.Zero) throw new Win32Exception();
        foreach (var manifest in manifests) {
            if (UpdateResource(update, Manifest, ProcessManifest, manifest.Key, manifest.Value, (uint)manifest.Value.Length))
                continue;
            var error = new Win32Exception();
            EndUpdateResource(update, true);
            throw error;
        }
        if (!EndUpdateResource(update, false)) throw new Win32Exception();
    }
}
'@
    }

    $path = (Get-Item -LiteralPath $Executable).FullName
    $resources = [DreamHouseLauncherResources]::Read($path)
    if ($resources.Count -eq 0) {
        $resources.Add([uint16]0x0409, [Text.Encoding]::UTF8.GetBytes('<assembly xmlns="urn:schemas-microsoft-com:asm.v1" manifestVersion="1.0"/>'))
    }
    foreach ($language in @($resources.Keys)) {
        $xml = New-Object Xml.XmlDocument
        $stream = [IO.MemoryStream]::new($resources[$language])
        try { $xml.Load($stream) } finally { $stream.Dispose() }
        $settingsNamespace = 'urn:schemas-microsoft-com:asm.v3'
        $codePageNamespace = 'http://schemas.microsoft.com/SMI/2019/WindowsSettings'
        $parent = $xml.DocumentElement
        foreach ($name in @('application','windowsSettings','activeCodePage')) {
            $namespace = $settingsNamespace
            if ($name -eq 'activeCodePage') { $namespace = $codePageNamespace }
            $element = $parent.GetElementsByTagName($name,$namespace) | Select-Object -First 1
            if (!$element) { $element = $parent.AppendChild($xml.CreateElement($name,$namespace)) }
            $parent = $element
        }
        $parent.InnerText = 'UTF-8'
        $encoded = [IO.MemoryStream]::new()
        try {
            $writerSettings = [Xml.XmlWriterSettings]::new()
            $writerSettings.Encoding = [Text.UTF8Encoding]::new($false)
            $writer = [Xml.XmlWriter]::Create($encoded,$writerSettings)
            try { $xml.Save($writer) } finally { $writer.Dispose() }
            $resources[$language] = $encoded.ToArray()
        } finally { $encoded.Dispose() }
    }
    [DreamHouseLauncherResources]::Write($path,$resources)
}
