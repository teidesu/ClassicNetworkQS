#!/usr/bin/env python3
"""Import Android 16 classic Internet tile icons; bake SignalDrawable's STATE_CUT."""
import base64
import hashlib
import json
import urllib.request
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
revision = 'android-16.0.0_r1'
base = f'https://android.googlesource.com/platform/frameworks/base/+/refs/tags/{revision}/'
output = root / 'app/src/main/res/drawable'
android = '{http://schemas.android.com/apk/res/android}'
ET.register_namespace('android', 'http://schemas.android.com/apk/res/android')
imports = {}
for level in range(5):
    imports[f'ic_wifi_{level}'] = f'core/res/res/drawable/ic_wifi_signal_{level}.xml'
    imports[f'ic_wifi_{level}_no_internet'] = f'packages/SettingsLib/res/drawable/ic_no_internet_wifi_signal_{level}.xml'
    imports[f'ic_cellular_{level}'] = f'core/res/res/drawable/ic_signal_cellular_{level}_4_bar.xml'
imports.update({
    'ic_ethernet': 'packages/SystemUI/res/drawable/stat_sys_ethernet_fully.xml',
    'ic_ethernet_no_internet': 'packages/SystemUI/res/drawable/stat_sys_ethernet.xml',
    'ic_disconnected': 'packages/SystemUI/res/drawable/ic_qs_no_internet_unavailable.xml',
    'ic_airplane': 'packages/SystemUI/res/drawable/ic_qs_no_internet_unavailable.xml',
    'ic_internet': 'packages/SystemUI/res/drawable/ic_qs_no_internet_available.xml',
})
manifest = {'revision': revision, 'icons': {}}
for name, source in imports.items():
    url = base + source + '?format=TEXT'
    data = base64.b64decode(urllib.request.urlopen(url).read())
    output.joinpath(name + '.xml').write_bytes(data)
    manifest['icons'][name] = {'source': source, 'source_sha256': hashlib.sha256(data).hexdigest()}
    if name.startswith('ic_cellular_'):
        vector = ET.fromstring(data)
        vector.set(android + 'autoMirrored', 'true')
        normal = ET.tostring(vector, encoding='unicode')
        # Keep upstream license/comments in both flattened states.
        header = data.decode().split('<vector', 1)[0]
        output.joinpath(name + '.xml').write_text(header + normal + '\n')
        children = list(vector)
        for child in children:
            vector.remove(child)
        group = ET.SubElement(vector, 'group')
        ET.SubElement(group, 'clip-path', {
            android + 'pathData': 'M0,0H24V7H17V24H0Z',
        })
        group.extend(children)
        ET.SubElement(vector, 'path', {
            android + 'fillColor': '@android:color/white',
            android + 'pathData': 'M20,10h2v8h-2z M20,20h2v2h-2z',
        })
        output.joinpath(name + '_no_internet.xml').write_text(header + ET.tostring(vector, encoding='unicode') + '\n')
        manifest['icons'][name]['adaptation'] = 'autoMirrored; STATE_CUT variant clips x=17..24,y=7..24 and adds config_signalAttributionPath'
root.joinpath('third_party/aosp/icons.json').write_text(json.dumps(manifest, indent=2) + '\n')

# QPR1 expressive assets, pinned independently of the classic set.
revision = '33b96ce8a122757002e5040ac59824bd7a262e00'
base = f'https://android.googlesource.com/platform/frameworks/base/+/{revision}/'
imports = {}
for level, source_level in enumerate([0, 1, 2, 2, 3]):
    imports[f'ic_qpr1_wifi_{level}'] = f'packages/SettingsLib/res/drawable/ic_wifi_{source_level}.xml'
    imports[f'ic_qpr1_wifi_{level}_no_internet'] = f'packages/SettingsLib/res/drawable/ic_wifi_{source_level}_error.xml'
for level in range(5):
    imports[f'ic_qpr1_cellular_{level}'] = f'packages/SettingsLib/res/drawable/ic_mobile_{level}_4_bar.xml'
    imports[f'ic_qpr1_cellular_{level}_no_internet'] = f'packages/SettingsLib/res/drawable/ic_mobile_{level}_4_bar_error.xml'
imports['ic_networks_available'] = 'packages/SystemUI/res/drawable/ic_qs_no_internet_available.xml'
manifest = {'branch': 'android16-qpr1-release', 'revision': revision, 'icons': {}}
for name, source in imports.items():
    data = base64.b64decode(urllib.request.urlopen(base + source + '?format=TEXT').read())
    if 'cellular' in name:
        vector = ET.fromstring(data)
        vector.set(android + 'autoMirrored', 'true')
        header = data.decode().split('<vector', 1)[0]
        output.joinpath(name + '.xml').write_text(header + ET.tostring(vector, encoding='unicode') + '\n')
    else:
        output.joinpath(name + '.xml').write_bytes(data)
    manifest['icons'][name] = {'source': source, 'source_sha256': hashlib.sha256(data).hexdigest()}
root.joinpath('third_party/aosp/qpr1-icons.json').write_text(json.dumps(manifest, indent=2) + '\n')
