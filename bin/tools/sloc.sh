#bin

THIS=$(dirname "$0")
THIS=$(cd "$THIS">/dev/null || exit; pwd)

 . "$THIS"/../include/config.sh

cd "$APP_HOME"
if [ -e "$APP_HOME/../pom.xml" ]; then
  cd "$APP_HOME/.."
fi

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
