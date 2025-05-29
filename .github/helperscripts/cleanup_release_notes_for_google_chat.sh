#!/bin/bash

# Function to process release notes
process_release_notes() {
    local input="$1"
    
    # Process the input text
    # First convert literal \n to actual newlines, then process
    echo -e "$input" | sed -E '
        # Convert ## Headers to *Bold*
        s/^## (.*)$/\*\1\*/g

        # Remove complete URLs from markdown links: [text](url) -> [text]
        s/\]\([^)]*\)/]/g

        # Handle truncated cases at the end of input
        # Case 1: ends with incomplete URL like ](https://github.com/dolr-ai/h
        s/\]\([^)]*$/]/g

        # Case 2: ends with ([Name](incomplete_url -> ([Name]
        s/\(\[([^]]*)\]\([^)]*$/(\[\1]/g
    '
}

# Main script logic
if [ $# -eq 0 ]; then
    # Read from stdin if no arguments
    input=$(cat)
    process_release_notes "$input"
elif [ $# -eq 1 ]; then
    # Process single argument as input string
    process_release_notes "$1"
else
    echo "Usage: $0 [input_string]"
    echo "       echo 'input' | $0"
    echo ""
    echo "If no input_string is provided, reads from stdin"
    exit 1
fi