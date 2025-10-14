import os
import xml.etree.ElementTree as ET
from pathlib import Path

def check_pom_relative_paths(start_dir):
    """
    Recursively finds all pom.xml files in a directory and checks
    the parent's relativePath.
    """
    for root, dirs, files in os.walk(start_dir):
        for file in files:
            if file == 'pom.xml':
                pom_path = Path(root) / file
                try:
                    tree = ET.parse(pom_path)
                    namespace = get_namespace(tree.getroot())
                    parent = tree.find(f'{{{namespace}}}parent')
                    if parent is not None:
                        relative_path_element = parent.find(f'{{{namespace}}}relativePath')
                        if relative_path_element is not None:
                            relative_path = relative_path_element.text
                            parent_pom_path = (pom_path.parent / relative_path).resolve()
                            if not parent_pom_path.exists() or not parent_pom_path.is_file():
                                print(f"ERROR: Invalid relativePath in {pom_path}")
                                print(f"  -> {relative_path} does not point to a valid pom.xml file.")
                                print(f"  -> Resolved to: {parent_pom_path}")
                            else:
                                print(f"OK: {pom_path}")
                except ET.ParseError:
                    print(f"ERROR: Could not parse {pom_path}")
                except Exception as e:
                    print(f"An unexpected error occurred with {pom_path}: {e}")

def get_namespace(element):
    """
    Extracts the namespace from the root element of an XML file.
    """
    m = re.match(r'\{.*\}', element.tag)
    return m.group(0)[1:-1] if m else ''

if __name__ == "__main__":
    import re
    # Assuming the script is run from the root of the browser4 project
    project_root = Path('.').resolve()
    print(f"Starting scan from: {project_root}")
    check_pom_relative_paths(project_root)
    print("Scan complete.")

