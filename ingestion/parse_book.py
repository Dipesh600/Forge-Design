import pdfplumber
import re
import json
import argparse
import os

def parse_book(pdf_path: str, output_path: str):
    """
    Parses a PDF design book into a list of chapters or distinct sections.
    Uses rudimentary heuristic to detect "Chapter" or bold headings.
    """
    if not os.path.exists(pdf_path):
        print(f"Error: {pdf_path} not found.")
        return

    chapters = []
    current_chapter = {"title": "Introduction", "text": ""}

    print(f"Parsing {pdf_path}...")
    
    with pdfplumber.open(pdf_path) as pdf:
        for i, page in enumerate(pdf.pages):
            text = page.extract_text()
            if not text:
                continue
                
            lines = text.split('\n')
            for line in lines:
                line_stripped = line.strip()
                if not line_stripped:
                    continue
                
                # Rudimentary chapter detection: "Chapter X", "Rule X", or ALL CAPS short lines
                if re.match(r'^(Chapter|Rule|Principle)\s+\d+', line_stripped, re.IGNORECASE) or \
                   (line_stripped.isupper() and len(line_stripped) < 40 and len(line_stripped) > 4):
                    
                    if current_chapter["text"].strip():
                        chapters.append(current_chapter)
                    current_chapter = {"title": line_stripped, "text": ""}
                else:
                    current_chapter["text"] += line_stripped + " "
                    
    if current_chapter["text"].strip():
        chapters.append(current_chapter)
        
    print(f"Found {len(chapters)} sections.")
    
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(chapters, f, indent=2)
        
    print(f"Saved to {output_path}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Parse PDF book into JSON chapters")
    parser.add_argument("pdf_path", help="Path to input PDF file")
    parser.add_argument("output_path", help="Path to output JSON file")
    args = parser.parse_args()
    
    parse_book(args.pdf_path, args.output_path)
