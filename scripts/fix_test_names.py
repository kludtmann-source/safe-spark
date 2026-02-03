#!/usr/bin/env python3
"""
Fix Android Test Function Names - Remove Backticks

Converts `fun `test name with spaces`()` to `fun testTestNameWithSpaces()`
"""

import re
import sys

def convert_function_name(match):
    """Convert backtick function name to camelCase"""
    name = match.group(1)
    # Remove special characters and convert to camelCase
    words = re.sub(r'[^a-zA-Z0-9]+', ' ', name).split()
    camel = 'test' + ''.join(word.capitalize() for word in words)
    return f'fun {camel}()'

def fix_file(filepath):
    """Fix all backtick function names in a file"""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Pattern: fun `anything`()
    pattern = r'fun `([^`]+)`\(\)'
    fixed = re.sub(pattern, convert_function_name, content)
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(fixed)
    
    print(f"✅ Fixed: {filepath}")

if __name__ == '__main__':
    files = [
        'app/src/androidTest/java/com/example/safespark/detection/SemanticDetectorTest.kt',
        'app/src/androidTest/java/com/example/safespark/SafeSparkEngineSemanticTest.kt'
    ]
    
    for f in files:
        fix_file(f)
    
    print("\n✅ All test function names converted!")
