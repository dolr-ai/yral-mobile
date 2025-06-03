#!/bin/bash

# Function to process release notes
process_release_notes() {
    local input="$1"

    # Process the input text
    # First convert literal \n to actual newlines for processing
    echo -e "$input" | sed -E '
        # Convert ## Headers to <b>Bold</b>
        s/^## (.*)$/<b>\1<\/b>/g

        # Convert markdown links [text](url) to <a href="url">text</a>
        s/\[([^]]*)\]\(([^)]*)\)/<a href="\2">\1<\/a>/g

        # Handle truncated links at the end - remove incomplete ones
        s/\[([^]]*)\]\([^)]*$/<a href="">\1<\/a>/g
    ' | sed ':a;N;$!ba;s/\n/<br>/g'
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