import os
import shutil

def rename_package(src_dir, old_pkg, new_pkg):
    old_path = os.path.join(src_dir, *old_pkg.split('.'))
    new_path = os.path.join(src_dir, *new_pkg.split('.'))
    
    if os.path.exists(old_path):
        os.makedirs(new_path, exist_ok=True)
        for item in os.listdir(old_path):
            s = os.path.join(old_path, item)
            d = os.path.join(new_path, item)
            shutil.move(s, d)
        
        # Clean up old empty directories
        p = old_path
        while p != src_dir:
            if not os.listdir(p):
                os.rmdir(p)
            p = os.path.dirname(p)

for d in ['app/src/main/java', 'app/src/androidTest/java', 'app/src/test/java']:
    rename_package(d, 'com.meta.portal.sampleapp', 'com.familytime')

ui_dir = 'app/src/main/java/com/familytime/ui'
if os.path.exists(ui_dir):
    shutil.rmtree(ui_dir)

files_to_remove = [
    'PermissionsSection.kt',
    'CameraSection.kt',
    'AudioRecorderSection.kt',
    'UiElementsSection.kt'
]

for f in files_to_remove:
    p = os.path.join('app/src/main/java/com/familytime', f)
    if os.path.exists(p):
        os.remove(p)
