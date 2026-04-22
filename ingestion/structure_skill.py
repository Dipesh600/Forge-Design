import json
import requests
import argparse
import os
from dotenv import load_dotenv

load_dotenv()

MINIMAX_API_KEY = os.getenv("MINIMAX_API_KEY")
if not MINIMAX_API_KEY:
    print("Warning: MINIMAX_API_KEY not found in .env")

def structure_skill(input_json: str, output_json: str, skill_name: str, book_name: str):
    """
    Takes a parsed book JSON, sends chunks to MiniMax to extract explicit 
    design rules, and saves them as a structured Skill JSON.
    """
    with open(input_json, 'r', encoding='utf-8') as f:
        chapters = json.load(f)
        
    skill_rules = []
    
    print(f"Extracting rules for '{skill_name}' from {len(chapters)} sections...")

    # For safety, we only process first few chapters to avoid massive API bills in this script
    # In production you'd batch this or process selectively.
    for i, chapter in enumerate(chapters[:10]):
        text = chapter.get("text", "")
        if len(text) < 200:
            continue
            
        print(f"  Processing section {i+1}...")
        
        prompt = f"""
You are an expert design systems engineer. Extract concrete, machine-checkable design rules from the following text chapter.

Output MUST be a valid JSON array of objects, with NO markdown formatting, NO markdown code blocks, just raw JSON.
If no concrete rules exist in this text, output an empty array: []

Schema for each object:
{{
  "id": "short-kebab-case-id",
  "rule": "Detailed rule description",
  "severity": "ERROR" or "WARNING",
  "checkType": "STRUCTURAL" or "SEMANTIC"
}}

Skill Context: {skill_name}

Text:
{text[:6000]}
        """
        
        headers = {
            "Authorization": f"Bearer {MINIMAX_API_KEY}",
            "Content-Type": "application/json"
        }
        
        payload = {
            "model": "MiniMax-Text-01",
            "messages": [{"role": "user", "content": prompt}],
            "temperature": 0.1
        }
        
        try:
            resp = requests.post("https://api.minimax.chat/v1/text/chatcompletion_v2", headers=headers, json=payload)
            resp.raise_for_status()
            
            content = resp.json()["choices"][0]["message"]["content"]
            # Clean up markdown if model ignored the prompt
            content = content.strip().removeprefix("```json").removeprefix("```").removesuffix("```").strip()
            
            rules = json.loads(content)
            if isinstance(rules, list):
                skill_rules.extend(rules)
                
        except Exception as e:
            print(f"Failed on section {i+1}: {e}")
            
    final_skill = {
        "id": skill_name.lower().replace(" ", "-"),
        "name": skill_name,
        "version": "1.0",
        "sourceBooks": [book_name],
        "rules": skill_rules,
        "embedding": [] # To be filled by embed_and_index.py
    }
    
    with open(output_json, 'w', encoding='utf-8') as f:
        json.dump(final_skill, f, indent=2)
        
    print(f"Extracted {len(skill_rules)} rules. Saved to {output_json}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Extract skill rules from parsed book JSON")
    parser.add_argument("input_json", help="Path to parsed book JSON")
    parser.add_argument("output_json", help="Path to output Skill JSON")
    parser.add_argument("skill_name", help="Name of the skill to extract (e.g. 'Visual Hierarchy')")
    parser.add_argument("book_name", help="Name of the source book")
    args = parser.parse_args()
    
    structure_skill(args.input_json, args.output_json, args.skill_name, args.book_name)
