import re
import sys

def extract_curl_blocks(file_path):
    """
    Extract all shell code blocks that contain curl commands from a markdown file.
    Returns a list of strings, each containing a curl command block.
    """
    try:
        with open(file_path, 'r', encoding='utf-8') as file:
            content = file.read()

        # Pattern to match shell code blocks
        pattern = r'```shell\n(.*?)\n```'

        # Find all matches
        shell_blocks = re.findall(pattern, content, re.DOTALL)

        # Filter blocks that contain curl commands
        curl_blocks = []
        for block in shell_blocks:
            block = block.strip()
            if 'curl' in block.lower():
                # Clean up the block by removing extra whitespace and newlines
                block = re.sub(r'\n\s*\\\s*', ' ', block)  # Join lines with backslash
                block = re.sub(r'\s+', ' ', block)  # Normalize whitespace
                curl_blocks.append(block)

        return curl_blocks

    except FileNotFoundError:
        print(f"Error: File '{file_path}' not found.", file=sys.stderr)
        return []
    except Exception as e:
        print(f"An error occurred: {str(e)}", file=sys.stderr)
        return []


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python extract_curl_blocks.py <markdown_file>", file=sys.stderr)
        sys.exit(1)
        
    file_path = sys.argv[1]
    curl_blocks = extract_curl_blocks(file_path)
    
    # When run directly, print each block on a new line
    for block in curl_blocks:
        print(block)
