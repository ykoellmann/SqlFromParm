# SqlFromParm
JetBrains plugin for evaluating SQL parameter objects (e.g. ParmBuilder) at debug time and generating the final SQL with all values replaced.

During a debugging session, the plugin extracts the raw SQL query and all associated parameters, replaces the placeholders (e.g. `@ParmX`) with their actual values, and copies the final SQL to the clipboard — ready to run.

---

## ✨ Features

- ✅ Extracts parameterized SQL strings and associated values from the debugger
- 🔍 Replaces placeholders like `@Parm1` with real values
- 🧠 Detects and formats `DateTime` values as `yyyy-MM-dd-HH.mm.ss.SSSSSS`
- 📋 Automatically copies the final SQL to the clipboard
- 💡 Works with nested parameter structures like `ParmBuilder`

---

## 🛠 How to Use

1. **Start debugging** a project that builds SQL via a parameter object like `ParmBuilder`.
2. In the **Variables** view, select:
    - the SQL string (e.g. `sql = "SELECT * FROM Users WHERE ..."`), and
    - the parameter object (e.g. `ParmBuilder`).
3. Use the shortcut `Ctrl+R, C`.
4. The plugin will:
    - collect all parameters,
    - replace them in the SQL string,
    - and copy the result to your clipboard.

---

## 🧪 Example

```csharp
var parm = new ParmBuilder();
var sql = "SELECT * FROM Users WHERE CreatedAt > {parm.AddParm(new DateTime(2024, 1, 1))}";
```
Result (in clipboard):

```sql
SELECT * FROM Users WHERE CreatedAt > 2024-01-01-00.00.00.000000
```

## ⌨️ Keyboard Shortcut
Ctrl + R, C — Evaluate Parm Variable
(Customizable via Settings → Keymap)

## 🧠 Author
Developed by Yannik Köllmann

## 📄 License

This project is licensed under the **Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)** license.

You are free to use, modify, and share this plugin for personal, educational, and open-source purposes.

**Commercial use is not allowed** without explicit permission.

See the [LICENSE](LICENSE) file for full terms.  
Learn more: [https://creativecommons.org/licenses/by-nc/4.0](https://creativecommons.org/licenses/by-nc/4.0)