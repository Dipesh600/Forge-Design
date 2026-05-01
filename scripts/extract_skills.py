#!/usr/bin/env python3
"""
extract_skills.py

Converts the rich SKILL.md files in .agents/skills/ into JSON assets
that the FORGE Android app loads via SkillAssetLoader.

Run from the project root:
    python3 scripts/extract_skills.py

This replaces the generic placeholder JSONs in assets/skills/ with
actual rules extracted from the canonical SKILL.md knowledge base.
"""

import json
import os
import re
import sys

# ── Config ────────────────────────────────────────────────────────────────────

SKILLS_DIR = os.path.join(os.path.dirname(__file__), '..', '.agents', 'skills')
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'assets', 'skills')

# Only convert design skills relevant to FORGE's UI generation pipeline.
# Exclude firebase/genkit/stitch-workflow skills — those are tool-use skills, not design rules.
DESIGN_SKILL_DIRS = {
    'android-platform-fluency': {
        'id': 'android-platform-fluency-v1',
        'category': 'android',
        'sourceBooks': ['Material Design 3', 'Android Developer Guidelines', 'WCAG 2.2'],
        'keywords': ['android', 'mobile', 'app', 'material', 'm3', 'screen', 'design', 'ui']
    },
    'color-systems': {
        'id': 'color-systems-v1',
        'category': 'color',
        'sourceBooks': ['Interaction of Color (Albers)', 'Material Design 3', 'Apple HIG'],
        'keywords': ['color', 'palette', 'dark', 'light', 'contrast', 'brand', 'theme']
    },
    'typography-systems': {
        'id': 'typography-systems-v1',
        'category': 'typography',
        'sourceBooks': ['The Elements of Typographic Style', 'Material Design 3', 'WCAG 2.2'],
        'keywords': ['text', 'font', 'type', 'typography', 'readable', 'heading', 'body']
    },
    'spatial-systems': {
        'id': 'spatial-systems-v1',
        'category': 'spatial',
        'sourceBooks': ['Grid Systems in Graphic Design', 'Material Design 3', '8-Point Grid'],
        'keywords': ['layout', 'spacing', 'grid', 'padding', 'margin', 'alignment', 'structure']
    },
    'visual-hierarchy': {
        'id': 'visual-hierarchy-v1',
        'category': 'hierarchy',
        'sourceBooks': ['Thinking with Type', 'Gestalt Principles', 'Visual Design of the User Interface'],
        'keywords': ['hierarchy', 'emphasis', 'priority', 'attention', 'visual', 'weight', 'prominence']
    },
    'interaction-patterns': {
        'id': 'interaction-patterns-v1',
        'category': 'interaction',
        'sourceBooks': ['Designing Interfaces', 'Material Design 3', 'iOS HIG'],
        'keywords': ['button', 'tap', 'interaction', 'state', 'loading', 'empty', 'error', 'feedback']
    },
    'cognitive-load': {
        'id': 'cognitive-load-v1',
        'category': 'cognitive',
        'sourceBooks': ["Don't Make Me Think", 'Laws of UX', 'The Design of Everyday Things'],
        'keywords': ['usability', 'simple', 'intuitive', 'confusing', 'mental', 'friction', 'navigation']
    },
    'emotional-design': {
        'id': 'emotional-design-v1',
        'category': 'emotional',
        'sourceBooks': ['Emotional Design (Norman)', 'The Hook Model', 'Laws of UX'],
        'keywords': ['delight', 'engagement', 'onboarding', 'premium', 'feel', 'personality', 'animation']
    },
    'microcopy': {
        'id': 'microcopy-v1',
        'category': 'microcopy',
        'sourceBooks': ['Microcopy: The Complete Guide', 'Strategic Writing for UX', 'Plain Language Guidelines'],
        'keywords': ['text', 'label', 'copy', 'message', 'button', 'error', 'placeholder', 'title']
    },
}

# Rule weight by position in the Rules section (first rules = most fundamental = highest weight)
def position_to_weight(index: int, total: int) -> float:
    if total == 0:
        return 0.8
    # First rule = 1.0, last rule = 0.55, linear scale
    return round(1.0 - (index / max(total - 1, 1)) * 0.45, 2)


def parse_skill_md(skill_dir_path: str) -> list[dict]:
    """Parse the Rules section of a SKILL.md into a list of rule dicts.
    Extracts the full rule anatomy: Rule + Why + Check + Bad example + Good example.
    These are what make rules actionable for an LLM — without examples, rules stay abstract.
    """
    skill_md_path = os.path.join(skill_dir_path, 'SKILL.md')
    if not os.path.exists(skill_md_path):
        print(f"  ⚠ No SKILL.md at {skill_md_path}")
        return []

    with open(skill_md_path, 'r', encoding='utf-8') as f:
        content = f.read()

    rules = []

    # Extract all ### Rn: ... rule blocks
    rule_blocks = re.split(r'\n### R\d+:', content)

    for block in rule_blocks[1:]:
        lines = block.strip().split('\n')
        title = lines[0].strip() if lines else ''

        def extract_field(pattern, text):
            m = re.search(pattern, text, re.DOTALL | re.IGNORECASE)
            if m:
                # Take only first line/sentence to keep it concise
                raw = m.group(1).strip()
                # Stop at next ** field or end of block
                raw = re.split(r'\n\*\*', raw)[0].strip()
                return raw.replace('\n', ' ')
            return ''

        rule_text  = extract_field(r'\*\*Rule\*\*:\s*(.+?)(?=\n\*\*|\Z)', block)
        why_text   = extract_field(r'\*\*Why\*\*:\s*(.+?)(?=\n\*\*|\Z)', block)
        check_text = extract_field(r'\*\*Check\*\*:\s*(.+?)(?=\n\*\*|\Z)', block)
        bad_text   = extract_field(r'\*\*Bad example\*\*:\s*(.+?)(?=\n\*\*|\Z)', block)
        good_text  = extract_field(r'\*\*Good example\*\*:\s*(.+?)(?=\n\*\*|\Z)', block)

        if rule_text:
            rules.append({
                'title':   title,
                'rule':    rule_text,
                'why':     why_text[:200] if why_text else '',
                'check':   check_text[:150] if check_text else '',
                'bad':     bad_text[:120] if bad_text else '',
                'good':    good_text[:120] if good_text else '',
            })

    return rules


def extract_description(skill_dir_path: str) -> str:
    """Extract the description from the YAML front matter of SKILL.md."""
    skill_md_path = os.path.join(skill_dir_path, 'SKILL.md')
    if not os.path.exists(skill_md_path):
        return ''
    with open(skill_md_path, 'r', encoding='utf-8') as f:
        content = f.read()
    match = re.search(r'^description:\s*(.+?)$', content, re.MULTILINE)
    return match.group(1).strip() if match else ''


def convert_skill(skill_folder_name: str, config: dict) -> dict:
    """Convert a SKILL.md folder to a JSON dict for SkillAssetLoader."""
    skill_dir = os.path.join(SKILLS_DIR, skill_folder_name)
    raw_rules = parse_skill_md(skill_dir)
    total = len(raw_rules)

    rules_json = []
    for i, r in enumerate(raw_rules):
        weight = position_to_weight(i, total)
        rule_id = f"{config['id']}-r{i+1}"
        entry = {
            'id':     rule_id,
            'rule':   r['rule'],
            'weight': weight
        }
        # Include the full rule anatomy — these make the rule concrete for the LLM
        if r.get('why'):   entry['why']   = r['why']
        if r.get('check'): entry['check'] = r['check']
        if r.get('bad'):   entry['bad']   = r['bad']
        if r.get('good'):  entry['good']  = r['good']
        rules_json.append(entry)

    description = extract_description(skill_dir)

    return {
        'id': config['id'],
        'name': skill_folder_name.replace('-', ' ').title(),
        'version': '3.0',
        'category': config['category'],
        'description': description,
        'keywords': config.get('keywords', []),
        'sourceBooks': config['sourceBooks'],
        'embedding': [],
        'rules': rules_json
    }


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    print(f"Reading skills from: {os.path.abspath(SKILLS_DIR)}")
    print(f"Writing JSON to:     {os.path.abspath(OUTPUT_DIR)}\n")

    total_rules = 0
    for folder_name, config in DESIGN_SKILL_DIRS.items():
        skill_dir = os.path.join(SKILLS_DIR, folder_name)
        if not os.path.isdir(skill_dir):
            print(f"  ⚠ Skill dir not found: {skill_dir}")
            continue

        print(f"Processing: {folder_name}")
        skill_json = convert_skill(folder_name, config)
        rule_count = len(skill_json['rules'])
        total_rules += rule_count

        out_file = os.path.join(OUTPUT_DIR, f"{folder_name}.json")
        with open(out_file, 'w', encoding='utf-8') as f:
            json.dump(skill_json, f, indent=2, ensure_ascii=False)

        print(f"  ✓ {rule_count} rules → {os.path.basename(out_file)}")

    print(f"\n✅ Done. {len(DESIGN_SKILL_DIRS)} skills, {total_rules} total rules extracted.")
    print(f"The old placeholder JSONs have been replaced.")


if __name__ == '__main__':
    main()
