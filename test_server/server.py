from flask import Flask, request, jsonify
import base64
import os
from datetime import datetime

app = Flask(__name__)

# Create uploads folder if not exists
UPLOAD_FOLDER = 'uploads'
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

@app.route('/api/upload', methods=['POST'])
def upload_image():
    try:
        data = request.get_json()
        
        if not data:
            return jsonify({'error': 'No JSON data received'}), 400
        
        image_id = data.get('id')
        image_base64 = data.get('image')
        created_at = data.get('created_at')
        
        print(f"\n{'='*50}")
        print(f"Received image upload request:")
        print(f"  ID: {image_id}")
        print(f"  Created at: {created_at}")
        print(f"  Image size: {len(image_base64) if image_base64 else 0} chars (base64)")
        
        if not image_base64:
            return jsonify({'error': 'No image data'}), 400
        
        # Decode Base64 image
        image_bytes = base64.b64decode(image_base64)
        
        # Save image to file
        filename = f"mango_{image_id}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.png"
        filepath = os.path.join(UPLOAD_FOLDER, filename)
        
        with open(filepath, 'wb') as f:
            f.write(image_bytes)
        
        print(f"  Saved to: {filepath}")
        print(f"  File size: {len(image_bytes)} bytes")
        print(f"{'='*50}\n")
        
        return jsonify({
            'success': True,
            'message': 'Image uploaded successfully',
            'filename': filename
        }), 200
        
    except Exception as e:
        print(f"Error: {str(e)}")
        return jsonify({'error': str(e)}), 500

@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({'status': 'ok'}), 200

if __name__ == '__main__':
    print("\n" + "="*50)
    print("Flask Test Server for Mango Image Upload")
    print("="*50)
    print(f"Upload endpoint: http://172.31.246.40:5000/api/upload")
    print(f"Health check:    http://172.31.246.40:5000/health")
    print(f"Images saved to: ./{UPLOAD_FOLDER}/")
    print("="*50 + "\n")
    
    # Run on 0.0.0.0 to accept connections from other devices
    app.run(host='0.0.0.0', port=5000, debug=True)
