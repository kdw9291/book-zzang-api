#!/usr/bin/env bash
set -euo pipefail

readonly AWS_REGION="${AWS_REGION:-ap-northeast-2}"
readonly PARAMETER_PATH="${PARAMETER_PATH:-/bookzzang/prod/}"
readonly APPLICATION_JAR="${APPLICATION_JAR:-/opt/bookzzang/bookzzang-api.jar}"

parameters_json="$(aws ssm get-parameters-by-path \
  --region "$AWS_REGION" \
  --path "$PARAMETER_PATH" \
  --with-decryption \
  --query 'Parameters[].{Name:Name,Value:Value}' \
  --output json)"

required_parameters=(
  DB_URL
  DB_USERNAME
  DB_PASSWORD
  KAKAO_API_KEY
  GOOGLE_BOOKS_API_KEY
  PII_ENCRYPTION_KEY
  PII_EMAIL_LOOKUP_KEY
  EMAIL_VERIFICATION_SECRET
  AWS_SES_SENDER
)

for parameter_name in "${required_parameters[@]}"; do
  full_name="${PARAMETER_PATH%/}/${parameter_name}"
  value="$(jq -er --arg name "$full_name" '.[] | select(.Name == $name) | .Value' <<<"$parameters_json")" || {
    echo "Required Parameter Store value is missing: $full_name" >&2
    exit 1
  }
  export "${parameter_name}=${value}"
done

unset parameters_json value
export AWS_SES_REGION="$AWS_REGION"

exec /usr/bin/java -jar "$APPLICATION_JAR"
