import re
import sys
import os

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
                # Clean up the block while preserving newlines
                # Remove trailing backslashes and join continuation lines
                block = re.sub(r'\\\s*\n\s*', ' ', block)
                # Remove extra spaces but preserve newlines
                block = re.sub(r'[ \t]+', ' ', block)
                # Remove spaces before newlines
                block = re.sub(r' \n', '\n', block)
                # Remove multiple consecutive newlines
                block = re.sub(r'\n+', '\n', block)
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
    
    # Create /tmp/curl directory if it doesn't exist
    temp_dir = '/tmp/curl'
    os.makedirs(temp_dir, exist_ok=True)
    
    # Write each block to a separate file
    for i, block in enumerate(curl_blocks, 1):
        block_file = os.path.join(temp_dir, f'curl_block_{i}.sh')
        with open(block_file, 'w') as f:
            f.write(block)
        # Make the file executable
        os.chmod(block_file, 0o755)
        
    # Print the file path so it can be captured by the caller
    print(temp_dir)

