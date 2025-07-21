#!/bin/bash

# A script to generate the directory structure for the noqodi-support-hub.
# It's designed to be easily extensible for new sections and articles.

# --- Configuration ---
# Define the root directory name
ROOT_DIR="noqodi-support-hub"

# Define the folder structure using an associative array.
# The key is the directory name, and the value is a space-separated list of file names (without .md).
declare -A FOLDER_STRUCTURE
FOLDER_STRUCTURE=(
    ["01-account-management"]="01-creating-your-account 02-resetting-your-password 03-updating-billing-info"
    ["02-making-payments"]="01-how-to-make-a-payment 02-understanding-payment-statuses 03-troubleshooting-failed-payments"
    ["03-refunds-and-disputes"]="01-requesting-a-refund 02-how-disputes-work 03-checking-refund-status"
    ["04-security-and-fraud"]="01-enabling-two-factor-auth 02-recognizing-phishing-scams 03-reporting-suspicious-activity"
    ["05-developer-and-api"]="01-getting-your-api-keys 02-understanding-webhooks 03-using-the-sandbox-environment"
)

# --- Script Execution ---
echo "Creating documentation structure in './$ROOT_DIR/'..."

# Create the root directory and navigate into it. Exit if creation fails.
mkdir -p "$ROOT_DIR"
cd "$ROOT_DIR" || exit

# Create the root index file
echo "# Welcome to the noqodi Support Hub" > index.md

# Loop through the defined structure to create directories and files
for dir in "${!FOLDER_STRUCTURE[@]}"; do
    echo "  Creating directory: $dir/"
    mkdir -p "$dir"

    # Read the file names for the current directory into an array
    read -r -a files <<< "${FOLDER_STRUCTURE[$dir]}"

    for file in "${files[@]}"; do
        filepath="$dir/$file.md"

        # Generate a nice title from the filename (e.g., "01-creating-your-account" -> "Creating Your Account")
        title=$(echo "$file" | sed -e 's/^[0-9]*-//' -e 's/-/ /g' -e 's/\b\(.\)/\u\1/g')

        # Create the markdown file with a placeholder H1 title
        echo "# $title" > "$filepath"
        echo "" >> "$filepath"
        echo "Content for $title goes here." >> "$filepath"
    done
done

echo ""
echo "Successfully created the folder structure for '$ROOT_DIR'."
echo "You can now start adding content to the markdown files."

# Go back to the original directory
cd ..