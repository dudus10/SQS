import re

input_file = "creates3bucket_state.tf"
output_file = "creates3bucket_state_no_backend.tf"

with open(input_file, "r") as f:
    content = f.read()

# Match the backend "s3" block only
backend_pattern = r'(?sm)backend\s*"s3"\s*{.*?}\n?'

# Remove the backend block
content_modified = re.sub(backend_pattern, '', content)

with open(output_file, "w") as f:
    f.write(content_modified)

print(f"✅ Backend block removed. Output saved to: {output_file}")
