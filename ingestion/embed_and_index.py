import json
import requests
import argparse
import os
from dotenv import load_dotenv

load_dotenv()

MINIMAX_API_KEY = os.getenv("MINIMAX_API_KEY")

def embed_skill(input_json: str, output_json: str):
    """
    Generates a vector embedding for the entire skill using MiniMax Embeddings API.
    This vector represents the semantic meaning of the skill and all its rules.
    """
    if not MINIMAX_API_KEY:
        print("Error: MINIMAX_API_KEY not found in .env")
        return

    with open(input_json, 'r', encoding='utf-8') as f:
        skill = json.load(f)
        
    # Construct a comprehensive document string representing the skill
    rules_text = "\n".join([f"- {r.get('rule', '')}" for r in skill.get("rules", [])])
    semantic_doc = f"Skill: {skill.get('name')}\nRules:\n{rules_text}"
    
    print(f"Generating embedding for {skill.get('name')} (Text length: {len(semantic_doc)} chars)...")
    
    headers = {
        "Authorization": f"Bearer {MINIMAX_API_KEY}",
        "Content-Type": "application/json"
    }
    
    payload = {
        "model": "embo-01",
        "texts": [semantic_doc],
        "type": "db"  # We are storing this to be searched against
    }
    
    try:
        resp = requests.post("https://api.minimax.chat/v1/embeddings", headers=headers, json=payload)
        resp.raise_for_status()
        
        # Format is {"vectors": [[0.1, 0.2, ...]]}
        vectors = resp.json().get("vectors", [])
        if vectors and len(vectors) > 0:
            skill["embedding"] = vectors[0]
            print(f"Successfully generated embedding with {len(skill['embedding'])} dimensions.")
        else:
            print("Error: No vectors returned from API.")
            return
            
    except Exception as e:
        print(f"Embedding failed: {e}")
        return
        
    with open(output_json, 'w', encoding='utf-8') as f:
        json.dump(skill, f, indent=2)
        
    print(f"Saved embedded skill to {output_json}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate embeddings for a structured skill")
    parser.add_argument("input_json", help="Path to structured Skill JSON")
    parser.add_argument("output_json", help="Path to output embedded Skill JSON")
    args = parser.parse_args()
    
    embed_skill(args.input_json, args.output_json)
