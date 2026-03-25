import sys
from PIL import Image

try:
    img_path = r'c:\Users\chenk\.gemini\antigravity\scratch\ultimate_projects\Echofy-android\app\src\main\ic_launcher-playstore.png'
    img = Image.open(img_path)
    print(f"Original size: {img.size}")
    
    target_size = (512, 512)
    if img.size != target_size:
        print(f"Resizing to {target_size}...")
        resized_img = img.resize(target_size, Image.Resampling.LANCZOS)
        output_path = r'C:\Users\chenk\.gemini\antigravity\brain\fefb255d-e21e-4618-a858-429bddbf168a\app_icon_512.png'
        resized_img.save(output_path)
        print(f"Saved resized image to: {output_path}")
    else:
        print("Image is already 512x512.")
        output_path = r'C:\Users\chenk\.gemini\antigravity\brain\fefb255d-e21e-4618-a858-429bddbf168a\app_icon_512.png'
        img.save(output_path) # Just copy it
        print(f"Saved (copied) image to: {output_path}")

except Exception as e:
    print(f"Error: {e}")
