import json
import argparse
import os
import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore

def deploy_skill(skill_json: str, config_path: str):
    """
    Deploys the finalized Skill JSON (with embeddings) to the Firestore Database.
    Requires a Google Services service account key JSON.
    """
    if not os.path.exists(config_path):
        print(f"Error: Firebase service account key not found at {config_path}")
        print("Download it from Firebase Console -> Project Settings -> Service Accounts")
        return

    # Initialize Firebase Admin
    cred = credentials.Certificate(config_path)
    if not firebase_admin._apps:
        firebase_admin.initialize_app(cred)
        
    db = firestore.client()
    
    with open(skill_json, 'r', encoding='utf-8') as f:
        skill = json.load(f)
        
    skill_id = skill.get("id")
    if not skill_id:
        print("Error: Skill JSON has no 'id' field.")
        return
        
    print(f"Deploying skill '{skill.get('name')}' ({skill_id}) to Firestore...")
    
    # Write to Firestore
    doc_ref = db.collection('skills').document(skill_id)
    doc_ref.set(skill)
    
    print(f"Successfully deployed {skill_id} to Firestore collection 'skills'.")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Deploy a skill to Firestore")
    parser.add_argument("skill_json", help="Path to embedded Skill JSON")
    parser.add_argument("--key", default="firebase-key.json", help="Path to Firebase Service Account JSON key (default: firebase-key.json)")
    args = parser.parse_args()
    
    deploy_skill(args.skill_json, args.key)
