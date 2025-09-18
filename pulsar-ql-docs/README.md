# Pulsar QL Documentation Website

This directory contains a comprehensive HTML documentation website for the Pulsar QL module, which provides X-SQL User Defined Functions (UDFs) for web data extraction and processing.

## 📁 Files Structure

```
pulsar-ql-docs/
├── index.html           # Main documentation homepage
├── functions.html       # Complete functions reference
├── dom-functions.html   # Detailed DOM functions documentation
├── styles.css          # Custom styles and theme
├── script.js           # Interactive functionality
└── README.md           # This file
```

## 🚀 Features

### Homepage (index.html)
- **Hero Section**: Introduction to Pulsar QL with call-to-action buttons
- **Feature Overview**: Key capabilities highlighting web scraping, data processing, and SQL integration
- **Function Categories**: Visual cards for each function category with function counts
- **Usage Examples**: Practical SQL examples for different use cases
- **Quick Reference**: Essential functions for immediate use

### Functions Reference (functions.html)
- **Sidebar Navigation**: Easy navigation between function categories
- **Comprehensive Documentation**: All UDF functions organized by category
- **Search Functionality**: Search and filter functions (with keyboard shortcuts)
- **Code Examples**: Copyable SQL examples with syntax highlighting
- **Interactive Features**: Smooth scrolling, tooltips, and responsive design

### DOM Functions Deep Dive (dom-functions.html)
- **Detailed Documentation**: Complete DOM function reference with parameters and return types
- **Usage Examples**: Extensive SQL examples for each function
- **Use Cases**: Practical applications for each function category
- **Tips Section**: Best practices and performance recommendations

## 🎨 Design Features

### Visual Design
- **Modern Bootstrap 5**: Responsive and mobile-first design
- **Custom CSS**: Professional styling with CSS custom properties
- **Syntax Highlighting**: Prism.js for beautiful code presentation
- **Smooth Animations**: Subtle transitions and hover effects

### Interactive Elements
- **Copy Code Buttons**: One-click code copying with visual feedback
- **Search Functionality**: Real-time function search with highlighting
- **Keyboard Navigation**: Alt + / to focus search, Escape to clear
- **Scroll to Top**: Smooth scrolling back to top functionality
- **Active Navigation**: Sidebar highlights current section

### Responsive Design
- **Mobile Optimized**: Works perfectly on all device sizes
- **Touch Friendly**: Large tap targets and mobile navigation
- **Flexible Layout**: Adapts to different screen sizes

## 📖 Function Categories Covered

### 1. DOM Functions
- **Document Loading**: `DOM.load()`, `DOM.fetch()`, `DOM.parse()`
- **Element Selection**: `DOM.select()`, `DOM.selectFirst()`, `DOM.selectNth()`
- **Content Extraction**: `DOM.text()`, `DOM.ownText()`, `DOM.wholeText()`
- **Attribute Functions**: `DOM.attr()`, `DOM.hrefs()`, `DOM.hasAttr()`
- **Navigation Functions**: `DOM.parent()`, `DOM.children()`, `DOM.siblingIndex()`
- **Validation Functions**: `DOM.hasText()`, `DOM.isNil()`, `DOM.isNotNil()`
- **Utility Functions**: `DOM.tagName()`, `DOM.html()`, `DOM.attr()`

### 2. String Functions (STR namespace)
- **String Validation**: `STR.isNumeric()`, `STR.isAlpha()`, `STR.isEmpty()`
- **String Manipulation**: `STR.capitalize()`, `STR.substring()`, `STR.replace()`
- **String Comparison**: `STR.equals()`, `STR.startsWith()`, `STR.contains()`
- **Advanced Operations**: `STR.split()`, `STR.trim()`, `STR.reverse()`

### 3. Array Functions (ARRAY namespace)
- **Array Operations**: `ARRAY.joinToString()`, `ARRAY.firstNotBlank()`, `ARRAY.firstNotEmpty()`

### 4. DateTime Functions (TIME namespace)
- **DateTime Extraction**: `TIME.firstDateTime()`, `TIME.firstMysqlDateTime()`

### 5. Metadata Functions (META namespace)
- **Page Information**: `META.get()`

### 6. Common Functions
- **URL Processing**: `getTopPrivateDomain()`
- **Regular Expressions**: `re1()`, `re2()`
- **Data Conversion**: `formatTimestamp()`, `makeArray()`, `toJson()`

## 🔧 Usage Examples

### Basic Web Scraping
```sql
SELECT
    DOM.text(DOM.selectFirst(dom, 'title')) as title,
    DOM.hrefs(dom) as links
FROM (
    SELECT DOM.load('https://example.com') as dom
) t;
```

### String Processing
```sql
SELECT
    STR.capitalize(STR.trim(text)) as clean_text,
    STR.isNumeric(price) as is_valid_price
FROM product_data;
```

### Data Extraction with Regex
```sql
SELECT
    re1(content, '\$([0-9.]+)') as price,
    re1(content, '([0-9]{4}-[0-9]{2}-[0-9]{2})') as date
FROM articles;
```

## 🌐 Browser Compatibility

- **Modern Browsers**: Chrome, Firefox, Safari, Edge (latest versions)
- **Mobile Browsers**: iOS Safari, Chrome Mobile, Samsung Internet
- **Responsive**: Works on all screen sizes from mobile to desktop

## 🚀 Getting Started

1. **Open the Documentation**: Start with `index.html` for an overview
2. **Browse Functions**: Use `functions.html` for complete reference
3. **Deep Dive**: Check `dom-functions.html` for detailed DOM documentation
4. **Search**: Use the search functionality to find specific functions
5. **Copy Examples**: Click the copy button on any code example

## 📱 Testing the Documentation

To view the documentation locally:

1. **Using Python HTTP Server**:
   ```bash
   cd /home/vincent/workspace/browser4/pulsar-ql-docs
   python3 -m http.server 8080
   # Open http://localhost:8080 in your browser
   ```

2. **Using Node.js HTTP Server**:
   ```bash
   cd /home/vincent/workspace/browser4/pulsar-ql-docs
   npx http-server -p 8080
   # Open http://localhost:8080 in your browser
   ```

3. **Direct File Access**: Simply open `index.html` in your web browser

## 🎯 Key Benefits

- **Comprehensive**: Covers all UDF functions with detailed documentation
- **Practical**: Real-world examples and use cases
- **Interactive**: Search, copy code, and smooth navigation
- **Professional**: Modern design with excellent user experience
- **Accessible**: Mobile-friendly and keyboard navigable
- **Maintainable**: Clean HTML, CSS, and JavaScript code

## 🔗 Integration with Pulsar QL

This documentation website is specifically designed for the Pulsar QL module and provides:
- Complete coverage of all UDF functions
- SQL examples that work with the H2 database
- Real-world web scraping scenarios
- Performance tips and best practices

## 📊 Documentation Statistics

- **50+ DOM Functions**: Comprehensive web scraping capabilities
- **100+ String Functions**: Complete text processing toolkit
- **5+ Array Functions**: Array manipulation utilities
- **5+ DateTime Functions**: Temporal data extraction
- **20+ Common Functions**: URL processing, regex, and utilities

The documentation provides a complete reference for all X-SQL functions available in the Pulsar QL module, making it easy for developers to leverage the full power of SQL-based web data extraction and processing.