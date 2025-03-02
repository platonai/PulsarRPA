#bin

# Find the first parent directory that contains a pom.xml file
APP_HOME=$(cd "$(dirname "$0")">/dev/null || exit; pwd)
while [[ "$APP_HOME" != "/" ]]; do
  if [[ -f "$APP_HOME/pom.xml" ]]; then
    break
  fi
  APP_HOME=$(dirname "$APP_HOME")
done

cd "$APP_HOME" || exit

echo "Count lines of code in directory: $(pwd)"

TMP_FILE="/tmp/sloc"-$RANDOM
for suffix in "*.kt" "*.java" "*.js" "*.xml"
do
  find . -name "$suffix" -not -path "*-third*" \
    -not -path "*-resources*" \
    -not -path "*/target/*" \
    -not -path "*/lib/*" \
    -not -path "*/static/*" -type f \
    -not -path "*/res/*" \
     | xargs cat \
     | sed '/^\s*#/d;/^\s*$/d' \
     | wc -l > $TMP_FILE
  echo "$suffix: $(cat "$TMP_FILE")"
done

cd - > /dev/null || exit
