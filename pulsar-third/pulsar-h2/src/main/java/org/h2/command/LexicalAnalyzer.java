package org.h2.command;

import org.h2.api.ErrorCode;
import org.h2.engine.DbSettings;
import org.h2.engine.Mode;
import org.h2.message.DbException;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;

/**
 * Created by vincent on 17-10-15.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
abstract class LexicalAnalyzer {

  // used during the tokenizer phase
  private static final int CHAR_END = 1, CHAR_VALUE = 2, CHAR_QUOTED = 3;
  private static final int CHAR_OTHER = 0, CHAR_NAME = 4, CHAR_SPECIAL_1 = 5, CHAR_SPECIAL_2 = 6;
  private static final int CHAR_STRING = 7, CHAR_DOT = 8, CHAR_DOLLAR_QUOTED_STRING = 9;

  // this are token types
  static final int KEYWORD = 1, IDENTIFIER = 2, PARAMETER = 3, END = 4, VALUE = 5;
  static final int EQUAL = 6, BIGGER_EQUAL = 7, BIGGER = 8;
  static final int SMALLER = 9, SMALLER_EQUAL = 10, NOT_EQUAL = 11, AT = 12;
  static final int MINUS = 13, PLUS = 14, STRING_CONCAT = 15;
  static final int OPEN = 16, CLOSE = 17, NULL = 18, TRUE = 19, FALSE = 20;

  static final int ROWNUM = 24;
  static final int SPATIAL_INTERSECTS = 25; // &&

  static final int DOWN_STREAM = 30;

  /**
   * Checks if this string is a SQL keyword.
   *
   * @param s                  the token to check
   * @param supportOffsetFetch if OFFSET and FETCH are keywords
   * @return true if it is a keyword
   */
  public static boolean isKeyword(String s, boolean supportOffsetFetch) {
    if (s == null || s.length() == 0) {
      return false;
    }
    return getSaveTokenType(s, supportOffsetFetch) != IDENTIFIER;
  }

  /**
   * @see org.h2.engine.DbSettings#databaseToUpper
   */
  protected final Mode mode;
  final boolean identifiersToUpper;

  /**
   * The whole sql parsed to the parser
   */
  final String originalSQL;
  /**
   * whether the sql has been annotated
   */
  private boolean annotated;
  /**
   * copy of originalSQL, with comments blanked out
   */
  String cleanSql;
  /**
   * cached array if chars from cleanSql
   */
  char[] cleanSqlChars;
  /**
   * indicates character-type for each char in cleanSql
   */
  private int[] characterTypes;

  /**
   *
   * */
  int currentTokenType;
  String currentToken;
  boolean currentTokenQuoted;
  Value currentValue;
  /**
   * index into cleanSql of previous token
   */
  int lastParseIndex;
  /**
   * index into cleanSql of current token
   */
  int parseIndex;

  /**
   * holds all expected but failed tokens
   */
  ArrayList<String> expectedList;

  LexicalAnalyzer(String sql, Mode mode, DbSettings settings) {
    this.originalSQL = sql == null ? "" : sql;
    this.mode = mode;
    this.identifiersToUpper = settings.databaseToUpper;
  }

  String getUnparsedSql() {
    return parseIndex >= originalSQL.length() ? "" : originalSQL.substring(parseIndex);
  }

  void debug(int n) {
    debug(String.valueOf(n));
  }

  void debug(String prefix) {
    System.out.println(prefix + "-[" + currentToken + "(" + currentTokenType + ")]");
  }

  boolean isAnnotated() {
    return annotated;
  }

  void annotate() {
    cleanSql = originalSQL;

    int len = originalSQL.length() + 1; // one more byte then original sql
    char[] command = new char[len];
    int[] types = new int[len];

    len--;
    // copy string to char array
    originalSQL.getChars(0, len, command, 0);
    command[len] = ' ';
    types[len] = END;

    boolean changed = false;
    int startLoop = 0;
    int lastType = CHAR_OTHER;

    // Annotate char by char
    for (int i = 0; i < len; i++) {
      char c = command[i];
      int type = CHAR_OTHER;
      switch (c) {
        case '/':
          if (command[i + 1] == '*') {
            // block comment
            changed = true;
            command[i] = ' ';
            command[i + 1] = ' ';
            startLoop = i;
            i += 2;

            // strip comments
            checkRunOver(i, len, startLoop);
            while (command[i] != '*' || command[i + 1] != '/') {
              command[i++] = ' ';
              checkRunOver(i, len, startLoop);
            }

            command[i] = ' ';
            command[i + 1] = ' ';
            i++;
          } else if (command[i + 1] == '/') {
            // single line comment
            changed = true;
            startLoop = i;
            while (true) {
              c = command[i];
              if (c == '\n' || c == '\r' || i >= len - 1) {
                break;
              }
              command[i++] = ' ';
              checkRunOver(i, len, startLoop);
            }
          } else {
            type = CHAR_SPECIAL_1;
          }
          break;
        case '-':
          if (command[i + 1] == '-') {
            // single line comment
            changed = true;
            startLoop = i;
            while (true) {
              c = command[i];
              if (c == '\n' || c == '\r' || i >= len - 1) {
                break;
              }
              command[i++] = ' ';
              checkRunOver(i, len, startLoop);
            }
          } else {
            type = CHAR_SPECIAL_1;
          }
          break;
        case '$':
          if (command[i + 1] == '$' && (i == 0 || command[i - 1] <= ' ')) {
            // dollar quoted string
            changed = true;
            command[i] = ' ';
            command[i + 1] = ' ';
            startLoop = i;
            i += 2;
            checkRunOver(i, len, startLoop);
            while (command[i] != '$' || command[i + 1] != '$') {
              types[i++] = CHAR_DOLLAR_QUOTED_STRING;
              checkRunOver(i, len, startLoop);
            }
            command[i] = ' ';
            command[i + 1] = ' ';
            i++;
          } else {
            if (lastType == CHAR_NAME || lastType == CHAR_VALUE) {
              // $ inside an identifier is supported
              type = CHAR_NAME;
            } else if (command[i + 1] >= '1' && command[i + 1] <= '9') {
              // but not at the start, to support PostgreSQL $1
              type = CHAR_SPECIAL_1;
            }
            else {
              // $ inside an identifier is supported
              type = CHAR_NAME;
            }
          }
          break;
        case '(':
        case ')':
        case '{':
        case '}':
        case '*':
        case ',':
        case ';':
        case '+':
        case '%':
        case '?':
        case '@':
        case ']':
          type = CHAR_SPECIAL_1;
          break;
        case '!':
        case '<':
        case '>':
        case '|':
        case '=':
        case ':':
        case '&':
        case '~':
          type = CHAR_SPECIAL_2;
          break;
        case '.':
          type = CHAR_DOT;
          break;
        case '\'':
          type = types[i] = CHAR_STRING;
          startLoop = i;
          while (command[++i] != '\'') {
            checkRunOver(i, len, startLoop);
          }
          break;
        case '[':
          if (mode.squareBracketQuotedNames) {
            // SQL Server alias for "
            command[i] = '"';
            changed = true;
            type = types[i] = CHAR_QUOTED;
            startLoop = i;
            while (command[++i] != ']') {
              checkRunOver(i, len, startLoop);
            }
            command[i] = '"';
          } else {
            type = CHAR_SPECIAL_1;
          }
          break;
        case '`':
          // MySQL alias for ", but not case sensitive
          command[i] = '"';
          changed = true;
          type = types[i] = CHAR_QUOTED;
          startLoop = i;
          while (command[++i] != '`') {
            checkRunOver(i, len, startLoop);

            // TODO: why not case sensitive? case sensitive for quoted string is just OK
            final boolean caseSensitiveForQuotedString = true;
            if (!caseSensitiveForQuotedString && identifiersToUpper) {
              command[i] = Character.toUpperCase(command[i]);
            }
          }
          command[i] = '"';
          break;
        case '\"':
          type = types[i] = CHAR_QUOTED;
          startLoop = i;
          while (command[++i] != '\"') {
            checkRunOver(i, len, startLoop);
          }
          break;
        case '_':
          type = CHAR_NAME;
          break;
        case '#':
          if (mode.supportPoundSymbolForColumnNames) {
            type = CHAR_NAME;
            break;
          }
        default:
          if (c >= 'a' && c <= 'z') {
            if (identifiersToUpper) {
              command[i] = (char) (c - ('a' - 'A'));
              changed = true;
            }
            type = CHAR_NAME;
          } else if (c >= 'A' && c <= 'Z') {
            type = CHAR_NAME;
          } else if (c >= '0' && c <= '9') {
            type = CHAR_VALUE;
          } else {
            if (c <= ' ' || Character.isSpaceChar(c)) {
              // whitespace
            } else if (Character.isJavaIdentifierPart(c)) {
              // Java language support
              type = CHAR_NAME;
              if (identifiersToUpper) {
                char u = Character.toUpperCase(c);
                if (u != c) {
                  command[i] = u;
                  changed = true;
                }
              }
            } else {
              type = CHAR_SPECIAL_1;
            }
          }
      }
      types[i] = type;
      lastType = type;
    }

    cleanSqlChars = command;
    types[len] = CHAR_END;
    characterTypes = types;
    if (changed) {
      cleanSql = new String(command);
    }

    annotated = true;
    parseIndex = 0;
  }

  public boolean isNextStatement() {
    return isToken(";");
  }

  public boolean isDownstream() {
    return isToken("=>");
  }

  public boolean isEnd() {
    return currentTokenType == END;
  }

  protected void readToken() {
    doReadToken();
    boolean debugToken = false;
    if (debugToken) {
      if (true || originalSQL.toUpperCase().startsWith("SELECT")) {
        System.out.print(currentToken + "  ");
        if (currentTokenType == END || currentTokenType == DOWN_STREAM) {
          System.out.println();
        }
      }
    }
  }

  private void doReadToken() {
    currentTokenQuoted = false;
    if (expectedList != null) {
      expectedList.clear();
    }

    int[] types = characterTypes;
    lastParseIndex = parseIndex;

    int i = parseIndex;
    int type = types[i];
    // skip blanks
    while (type == CHAR_OTHER) {
      type = types[++i];
    }

    int start = i;
    char[] chars = cleanSqlChars;
    char c = chars[i++];
    currentToken = "";
    switch (type) {
      case CHAR_NAME:
        while (true) {
          type = types[i];
          if (type != CHAR_NAME && type != CHAR_VALUE) {
            break;
          }
          i++;
        }
        currentToken = StringUtils.cache(cleanSql.substring(start, i));
        currentTokenType = getTokenType(currentToken);
        parseIndex = i;
        return;
      case CHAR_QUOTED: {
        String result = null;
        while (true) {
          for (int begin = i; ; i++) {
            if (chars[i] == '\"') {
              if (result == null) {
                result = cleanSql.substring(begin, i);
              } else {
                result += cleanSql.substring(begin - 1, i);
              }
              break;
            }
          }
          if (chars[++i] != '\"') {
            break;
          }
          i++;
        }
        currentToken = StringUtils.cache(result);
        parseIndex = i;
        currentTokenQuoted = true;
        currentTokenType = IDENTIFIER;
        return;
      }
      case CHAR_SPECIAL_2:
        if (types[i] == CHAR_SPECIAL_2) {
          i++;
        }
        currentToken = cleanSql.substring(start, i);
        currentTokenType = getSpecialType(currentToken);
        parseIndex = i;
        return;
      case CHAR_SPECIAL_1:
        currentToken = cleanSql.substring(start, i);
        currentTokenType = getSpecialType(currentToken);
        parseIndex = i;
        return;
      case CHAR_VALUE:
        if (c == '0' && chars[i] == 'X') {
          // hex number
          long number = 0;
          start += 2;
          i++;
          while (true) {
            c = chars[i];
            if ((c < '0' || c > '9') && (c < 'A' || c > 'F')) {
              checkLiterals(false);
              currentValue = ValueInt.get((int) number);
              currentTokenType = VALUE;
              currentToken = "0";
              parseIndex = i;
              return;
            }
            number = (number << 4) + c - (c >= 'A' ? ('A' - 0xa) : ('0'));
            if (number > Integer.MAX_VALUE) {
              readHexDecimal(start, i);
              return;
            }
            i++;
          }
        }
        long number = c - '0';
        while (true) {
          c = chars[i];
          if (c < '0' || c > '9') {
            if (c == '.' || c == 'E' || c == 'L') {
              readDecimal(start, i);
              break;
            }
            checkLiterals(false);
            currentValue = ValueInt.get((int) number);
            currentTokenType = VALUE;
            currentToken = "0";
            parseIndex = i;
            break;
          }
          number = number * 10 + (c - '0');
          if (number > Integer.MAX_VALUE) {
            readDecimal(start, i);
            break;
          }
          i++;
        }
        return;
      case CHAR_DOT:
        if (types[i] != CHAR_VALUE) {
          currentTokenType = KEYWORD;
          currentToken = ".";
          parseIndex = i;
          return;
        }
        readDecimal(i - 1, i);
        return;
      case CHAR_STRING: {
        // TODO: what is the difference between CHAR_QUOTED and CHAR_STRING?
        String result = null;
        while (true) {
          for (int begin = i; ; i++) {
            if (chars[i] == '\'') {
              if (result == null) {
                result = cleanSql.substring(begin, i);
              } else {
                result += cleanSql.substring(begin - 1, i);
              }
              break;
            }
          }
          if (chars[++i] != '\'') {
            break;
          }
          i++;
        }
        currentToken = "'";
        checkLiterals(true);
        currentValue = ValueString.get(StringUtils.cache(result), mode.treatEmptyStringsAsNull);
        parseIndex = i;
        currentTokenType = VALUE;
        return;
      }
      case CHAR_DOLLAR_QUOTED_STRING: {
        String result = null;
        int begin = i - 1;
        while (types[i] == CHAR_DOLLAR_QUOTED_STRING) {
          i++;
        }
        result = cleanSql.substring(begin, i);
        currentToken = "'";
        checkLiterals(true);
        currentValue = ValueString.get(StringUtils.cache(result), mode.treatEmptyStringsAsNull);
        parseIndex = i;
        currentTokenType = VALUE;
        return;
      }
      case CHAR_END:
        currentToken = "";
        currentTokenType = END;
        parseIndex = i;
        return;
      default:
        throw getSyntaxError();
    }
  }

  abstract void checkLiterals(boolean isText);

  private int getSpecialType(String s) {
    char c0 = s.charAt(0);
    if (s.length() == 1) {
      switch (c0) {
        case '?':
        case '$':
          return PARAMETER;
        case '@':
          return AT;
        case '+':
          return PLUS;
        case '-':
          return MINUS;
        case '{':
        case '}':
        case '*':
        case '/':
        case '%':
        case ';':
        case ',':
        case ':':
        case '[':
        case ']':
        case '~':
          return KEYWORD;
        case '(':
          return OPEN;
        case ')':
          return CLOSE;
        case '<':
          return SMALLER;
        case '>':
          return BIGGER;
        case '=':
          return EQUAL;
        default:
          break;
      }
    } else if (s.length() == 2) {
      switch (c0) {
        case ':':
          if ("::".equals(s)) {
            return KEYWORD;
          } else if (":=".equals(s)) {
            return KEYWORD;
          }
          break;
        case '>':
          if (">=".equals(s)) {
            return BIGGER_EQUAL;
          }
          break;
        case '<':
          if ("<=".equals(s)) {
            return SMALLER_EQUAL;
          } else if ("<>".equals(s)) {
            return NOT_EQUAL;
          }
          break;
        case '!':
          if ("!=".equals(s)) {
            return NOT_EQUAL;
          } else if ("!~".equals(s)) {
            return KEYWORD;
          }
          break;
        case '|':
          if ("||".equals(s)) {
            return STRING_CONCAT;
          }
          break;
        case '&':
          if ("&&".equals(s)) {
            return SPATIAL_INTERSECTS;
          }
          break;
        case '=':
          if ("=>".equals(s)) {
            return DOWN_STREAM;
          }
          break;
      }
    }

    throw getSyntaxError();
  }

  private int getTokenType(String s) {
    int len = s.length();
    if (len == 0) {
      throw getSyntaxError();
    }
    if (!identifiersToUpper) {
      // if not yet converted to uppercase, do it now
      s = StringUtils.toUpperEnglish(s);
    }
    return getSaveTokenType(s, mode.supportOffsetFetch);
  }

  boolean isKeyword(String s) {
    if (!identifiersToUpper) {
      // if not yet converted to uppercase, do it now
      s = StringUtils.toUpperEnglish(s);
    }
    return isKeyword(s, false);
  }

  private void checkRunOver(int i, int len, int startLoop) {
    if (i >= len) {
      parseIndex = startLoop;
      throw getSyntaxError();
    }
  }

  private static int getSaveTokenType(String s, boolean supportOffsetFetch) {
    switch (s.charAt(0)) {
      case 'C':
        if (s.equals("CHECK")) {
          return KEYWORD;
        }
        return getKeywordOrIdentifier(s, "CROSS", KEYWORD);
      case 'D':
        return getKeywordOrIdentifier(s, "DISTINCT", KEYWORD);
      case 'E':
        if ("EXCEPT".equals(s)) {
          return KEYWORD;
        }
        return getKeywordOrIdentifier(s, "EXISTS", KEYWORD);
      case 'F':
        if ("FROM".equals(s)) {
          return KEYWORD;
        } else if ("FOR".equals(s)) {
          return KEYWORD;
        } else if ("FULL".equals(s)) {
          return KEYWORD;
        } else if (supportOffsetFetch && "FETCH".equals(s)) {
          return KEYWORD;
        }
        return getKeywordOrIdentifier(s, "FALSE", FALSE);
      case 'G':
        return getKeywordOrIdentifier(s, "GROUP", KEYWORD);
      case 'H':
        return getKeywordOrIdentifier(s, "HAVING", KEYWORD);
      case 'I':
        if ("INNER".equals(s)) {
          return KEYWORD;
        } else if ("INTERSECT".equals(s)) {
          return KEYWORD;
        }
        return getKeywordOrIdentifier(s, "IS", KEYWORD);
      case 'J':
        return getKeywordOrIdentifier(s, "JOIN", KEYWORD);
      case 'L':
        if ("LIMIT".equals(s)) {
          return KEYWORD;
        }
        return getKeywordOrIdentifier(s, "LIKE", KEYWORD);
      case 'M':
        return getKeywordOrIdentifier(s, "MINUS", KEYWORD);
      case 'N':
        if ("NOT".equals(s)) {
          return KEYWORD;
        } else if ("NATURAL".equals(s)) {
          return KEYWORD;
        }
        return getKeywordOrIdentifier(s, "NULL", NULL);
      case 'O':
        if ("ON".equals(s)) {
          return KEYWORD;
        } else if (supportOffsetFetch && "OFFSET".equals(s)) {
          return KEYWORD;
        }
        return getKeywordOrIdentifier(s, "ORDER", KEYWORD);
      case 'P':
        return getKeywordOrIdentifier(s, "PRIMARY", KEYWORD);
      case 'R':
        return getKeywordOrIdentifier(s, "ROWNUM", ROWNUM);
      case 'S':
        return getKeywordOrIdentifier(s, "SELECT", KEYWORD);
      case 'T':
        return getKeywordOrIdentifier(s, "TRUE", TRUE);
      case 'U':
        if ("UNIQUE".equals(s)) {
          return KEYWORD;
        }
        return getKeywordOrIdentifier(s, "UNION", KEYWORD);
      case 'W':
        if ("WITH".equals(s)) {
          return KEYWORD;
        } else if ("WHICH".equals(s)) {
          return KEYWORD;
        }
        return getKeywordOrIdentifier(s, "WHERE", KEYWORD);
      default:
        return IDENTIFIER;
    }
  }

  private static int getKeywordOrIdentifier(String s1, String s2, int keywordType) {
    if (s1.equals(s2)) {
      return keywordType;
    }
    return IDENTIFIER;
  }

  protected void read(String expected) {
    if (currentTokenQuoted || !equalsToken(expected, currentToken)) {
      addExpected(expected);
      throw getSyntaxError();
    }
    readToken();
  }

  boolean readIf(String token) {
    if (!currentTokenQuoted && equalsToken(token, currentToken)) {
      readToken();
      return true;
    }
    addExpected(token);
    return false;
  }

  boolean isToken(String token) {
    boolean result = equalsToken(token, currentToken) && !currentTokenQuoted;
    if (result) {
      return true;
    }
    addExpected(token);
    return false;
  }

  boolean equalsToken(String a, String b) {
    if (a == null) {
      return b == null;
    } else if (a.equals(b)) {
      return true;
    } else if (!identifiersToUpper && a.equalsIgnoreCase(b)) {
      return true;
    }
    return false;
  }

  private void addExpected(String token) {
    if (expectedList != null) {
      expectedList.add(token);
    }
  }

  void readParameterIndex() {
    int i = parseIndex;

    char[] chars = cleanSqlChars;
    char c = chars[i++];
    long number = c - '0';
    while (true) {
      c = chars[i];
      if (c < '0' || c > '9') {
        currentValue = ValueInt.get((int) number);
        currentTokenType = VALUE;
        currentToken = "0";
        parseIndex = i;
        break;
      }
      number = number * 10 + (c - '0');
      if (number > Integer.MAX_VALUE) {
        throw DbException.getInvalidValueException("parameter index", number);
      }
      i++;
    }
  }

  private void readDecimal(int start, int i) {
    char[] chars = cleanSqlChars;
    int[] types = characterTypes;
    // go until the first non-number
    while (true) {
      int t = types[i];
      if (t != CHAR_DOT && t != CHAR_VALUE) {
        break;
      }
      i++;
    }
    boolean containsE = false;
    if (chars[i] == 'E' || chars[i] == 'e') {
      containsE = true;
      i++;
      if (chars[i] == '+' || chars[i] == '-') {
        i++;
      }
      if (types[i] != CHAR_VALUE) {
        throw getSyntaxError();
      }
      while (types[++i] == CHAR_VALUE) {
        // go until the first non-number
      }
    }
    parseIndex = i;
    String sub = cleanSql.substring(start, i);
    checkLiterals(false);
    if (!containsE && sub.indexOf('.') < 0) {
      BigInteger bi = new BigInteger(sub);
      if (bi.compareTo(ValueLong.MAX) <= 0) {
        // parse constants like "10000000L"
        if (chars[i] == 'L') {
          parseIndex++;
        }
        currentValue = ValueLong.get(bi.longValue());
        currentTokenType = VALUE;
        return;
      }
    }
    BigDecimal bd;
    try {
      bd = new BigDecimal(sub);
    } catch (NumberFormatException e) {
      throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, e, sub);
    }
    currentValue = ValueDecimal.get(bd);
    currentTokenType = VALUE;
  }

  private void readHexDecimal(int start, int i) {
    char[] chars = cleanSqlChars;
    char c;
    do {
      c = chars[++i];
    } while ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F'));
    parseIndex = i;
    String sub = cleanSql.substring(start, i);
    BigDecimal bd = new BigDecimal(new BigInteger(sub, 16));
    checkLiterals(false);
    currentValue = ValueDecimal.get(bd);
    currentTokenType = VALUE;
  }

  int readInt() {
//    boolean minus = false;
//    if (currentTokenType == MINUS) {
//      minus = true;
//      read();
//    } else if (currentTokenType == PLUS) {
//      read();
//    }
//    if (currentTokenType != VALUE) {
//      throw DbException.getSyntaxError(cleanSql, parseIndex, "integer");
//    }
//    if (minus) {
//      // must do that now, otherwise Integer.MIN_VALUE would not work
//      currentValue = currentValue.negate();
//    }
    readNumber("integer");
    int i = currentValue.getInt();
    readToken();
    return i;
  }

  int readPositiveInt() {
    int v = readInt();
    if (v < 0) {
      throw DbException.getInvalidValueException("positive integer", v);
    }
    return v;
  }

  long readLong() {
//    boolean minus = false;
//    if (currentTokenType == MINUS) {
//      minus = true;
//      read();
//    } else if (currentTokenType == PLUS) {
//      read();
//    }
//    if (currentTokenType != VALUE) {
//      throw DbException.getSyntaxError(cleanSql, parseIndex, "int");
//    }
//    if (minus) {
//      // must do that now, otherwise Long.MIN_VALUE would not work
//      currentValue = currentValue.negate();
//    }
    readNumber("long");
    long i = currentValue.getLong();
    readToken();
    return i;
  }

  private void readNumber(String message) {
    boolean minus = false;
    if (currentTokenType == MINUS) {
      minus = true;
      readToken();
    } else if (currentTokenType == PLUS) {
      readToken();
    }

    if (currentTokenType != VALUE) {
      throw DbException.getSyntaxError(cleanSql, parseIndex, message);
    }

    if (minus) {
      // must do that now, otherwise Long.MIN_VALUE would not work
      currentValue = currentValue.negate();
    }
  }

  boolean readBooleanSetting() {
    if (currentTokenType == VALUE) {
      boolean result = currentValue.getBoolean();
      readToken();
      return result;
    }
    if (readIf("TRUE") || readIf("ON")) {
      return true;
    } else if (readIf("FALSE") || readIf("OFF")) {
      return false;
    } else {
      throw getSyntaxError();
    }
  }

  DbException getSyntaxError() {
    if (expectedList == null || expectedList.size() == 0) {
      return DbException.getSyntaxError(cleanSql, parseIndex);
    }
    StatementBuilder buff = new StatementBuilder();
    for (String e : expectedList) {
      buff.appendExceptFirst(", ");
      buff.append(e);
    }
    return DbException.getSyntaxError(cleanSql, parseIndex, buff.toString());
  }

  public String getSqlAnnotation() {
    StringBuilder sb = new StringBuilder();
    for (char ch : cleanSqlChars) {
      sb.append(ch);
    }
    sb.append('\n');
    for (int type : characterTypes) {
      sb.append(type);
    }
    return sb.toString();
  }
}
