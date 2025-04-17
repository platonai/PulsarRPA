import re

def extract_shell_blocks(file_path):
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
                # block = re.sub(r'\s+', ' ', block)  # Normalize whitespace
                curl_blocks.append(block)

        return curl_blocks

    except FileNotFoundError:
        print(f"Error: File '{file_path}' not found.")
        return []
    except Exception as e:
        print(f"An error occurred: {str(e)}")
        return []


if __name__ == "__main__":
    print("Extracting all shell blocks:")
    extract_shell_blocks("README.md")
