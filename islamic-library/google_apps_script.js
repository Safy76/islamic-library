/**
 * =========================================================================
 * ISLAMIC BOOK LIBRARY SYSTEM - GOOGLE APPS SCRIPT WEB APP REST API
 * =========================================================================
 * 
 * Instructions:
 * 1. Open Google Sheets, create a new spreadsheet.
 * 2. Create seven sheets named exactly: "Books", "Volumes", "Categories", "Languages", "DarsBooks", "DarsClasses", "Admins".
 * 3. In the "Admins" sheet, create the following headers in row 1:
 *    Email | PasswordHash | Role | Status
 *    Insert a default admin row:
 *    safvandaya17@gmail.com | 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9 (hash of "admin123") | Super Admin | Active
 * 4. In the "Books" sheet, create headers in row 1:
 *    BookName | Author | Category | Language | Description | CoverImage | TotalVolumes | Featured | CreatedAt | UpdatedAt
 * 5. In the "Volumes" sheet, create headers in row 1:
 *    BookName | VolumeNumber | VolumeName | Thumbnail | PDF | FileSize | CreatedAt
 * 6. In the "Categories" sheet, create header in row 1:
 *    CategoryName
 * 7. In the "Languages" sheet, create header in row 1:
 *    LanguageName
 * 8. In the "DarsBooks" sheet, create headers in row 1:
 *    BookName | Author | DarsClass | Language | Description | CoverImage | TotalVolumes | Featured | CreatedAt | UpdatedAt
 * 9. In the "DarsClasses" sheet, create header in row 1:
 *    ClassName
 * 10. Open Extensions -> Apps Script. Delete any code and paste this script.
 * 11. Update the SPREADSHEET_ID variable below if needed (or let it bind automatically to the active sheet).
 * 12. Click "Deploy" -> "New Deployment" -> Select type: "Web App".
 *     - Execute as: "Me"
 *     - Who has access: "Anyone" (required for public REST API traffic).
 * 13. Copy the deployed Web App URL and paste it in `Config.kt`!
 */

var SPREADSHEET_ID = ""; // Leave blank to bind to Active Sheet, or insert your Sheet ID

function getSpreadsheet() {
  if (SPREADSHEET_ID && SPREADSHEET_ID !== "") {
    return SpreadsheetApp.openById(SPREADSHEET_ID);
  } else {
    return SpreadsheetApp.getActiveSpreadsheet();
  }
}

// Helper to convert sheet rows into JSON-ready arrays of objects
function sheetToJson(sheetName) {
  var sheet = getSpreadsheet().getSheetByName(sheetName);
  if (!sheet) return [];
  var data = sheet.getDataRange().getValues();
  if (data.length <= 1) return []; // Only header present
  
  var headers = data[0];
  var rows = [];
  
  for (var i = 1; i < data.length; i++) {
    var row = {};
    for (var j = 0; j < headers.length; j++) {
      var headerKey = String(headers[j]).trim();
      row[headerKey] = data[i][j];
    }
    rows.push(row);
  }
  return rows;
}

// Generate uniform JSON output response with CORS headers
function outputJson(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}

// =========================================================================
// GET HANDLERS (DO_GET)
// =========================================================================
function doGet(e) {
  try {
    if (!e || !e.parameter) {
      return ContentService.createTextOutput(JSON.stringify({ 
        success: false, 
        message: "Google Apps Script Web App works successfully! Note: Please do NOT click the 'Run' button inside the Google Apps Script editor for 'doGet' or 'doPost'. Google Apps Script does not pass any event arguments during manual runs, which causes errors. To use this API, please click 'Deploy' at the top right of the editor -> 'New Deployment' -> Select 'Web App' -> Execute as: 'Me' -> Who has access: 'Anyone' -> Click 'Deploy'. Then copy the 'Web App URL' and use it in your Android app." 
      })).setMimeType(ContentService.MimeType.JSON);
    }
    var action = e.parameter.action;
    
    if (!action) {
      return outputJson({ success: false, message: "No action parameter provided." });
    }
    
    if (action === "getBooks") {
      var books = sheetToJson("Books");
      // Map featured string/bool accurately
      books.forEach(function(b) {
        b.Featured = (b.Featured === true || b.Featured === "TRUE" || b.Featured === 1);
        b.TotalVolumes = parseInt(b.TotalVolumes) || 0;
      });
      return outputJson(books);
    }
    
    if (action === "getVolumes") {
      var bookName = e.parameter.bookName;
      var volumes = sheetToJson("Volumes");
      
      // Filter volumes matching bookName (BookName is the Relationship Key)
      var filtered = volumes.filter(function(v) {
        return String(v.BookName).trim().toLowerCase() === String(bookName).trim().toLowerCase();
      });
      
      filtered.forEach(function(v) {
        v.VolumeNumber = parseInt(v.VolumeNumber) || 1;
      });
      
      return outputJson(filtered);
    }
    
    if (action === "getCategories") {
      var categories = sheetToJson("Categories");
      return outputJson(categories);
    }
    
    if (action === "getLanguages") {
      var languages = sheetToJson("Languages");
      return outputJson(languages);
    }
    
    if (action === "getDarsBooks") {
      var darsBooks = sheetToJson("DarsBooks");
      // Map featured string/bool accurately
      darsBooks.forEach(function(b) {
        b.Featured = (b.Featured === true || b.Featured === "TRUE" || b.Featured === 1);
        b.TotalVolumes = parseInt(b.TotalVolumes) || 0;
      });
      return outputJson(darsBooks);
    }
    
    if (action === "getDarsClasses") {
      var classes = sheetToJson("DarsClasses");
      return outputJson(classes);
    }
    
    return outputJson({ success: false, message: "Action '" + action + "' not found on GET." });
    
  } catch (error) {
    return outputJson({ success: false, message: "GET Exception: " + error.toString() });
  }
}

// =========================================================================
// POST HANDLERS (DO_POST)
// =========================================================================
function doPost(e) {
  try {
    if (!e || !e.parameter) {
      return ContentService.createTextOutput(JSON.stringify({ 
        success: false, 
        message: "Google Apps Script Web App works successfully! Note: Please do NOT click the 'Run' button inside the Google Apps Script editor for 'doGet' or 'doPost'. Google Apps Script does not pass any event arguments during manual runs, which causes errors. To use this API, please click 'Deploy' at the top right of the editor -> 'New Deployment' -> Select 'Web App' -> Execute as: 'Me' -> Who has access: 'Anyone' -> Click 'Deploy'. Then copy the 'Web App URL' and use it in your Android app." 
      })).setMimeType(ContentService.MimeType.JSON);
    }
    var action = e.parameter.action;
    
    var postData = {};
    if (e.postData && e.postData.contents) {
      try {
        postData = JSON.parse(e.postData.contents);
      } catch (jsonErr) {
        // Safe fallback or ignore if empty
      }
    }
    
    if (!action) {
      return outputJson({ success: false, message: "No action specified on POST." });
    }
    
    var ss = getSpreadsheet();
    
    // 1. LOGIN AUTHENTICATION
    if (action === "login") {
      var email = postData.email;
      var passwordHash = postData.passwordHash; // Client passes pre-hashed SHA-256 string
      
      var admins = sheetToJson("Admins");
      var foundAdmin = admins.find(function(admin) {
        return String(admin.Email).trim().toLowerCase() === String(email).trim().toLowerCase() &&
               String(admin.PasswordHash).trim().toLowerCase() === String(passwordHash).trim().toLowerCase();
      });
      
      if (foundAdmin) {
        if (foundAdmin.Status === "Inactive") {
          return outputJson({ success: false, message: "Your admin account has been suspended." });
        }
        // Return session success
        return outputJson({
          success: true,
          message: "Authorized",
          token: "gas_session_" + Utilities.getUuid(),
          email: foundAdmin.Email,
          role: foundAdmin.Role
        });
      } else {
        return outputJson({ success: false, message: "Invalid email credentials or password." });
      }
    }
    
    // 2. ADD BOOK
    if (action === "addBook") {
      var sheet = ss.getSheetByName("Books");
      var nowStr = new Date().toISOString();
      
      // Append row matching column: BookName | Author | Category | Language | Description | CoverImage | TotalVolumes | Featured | CreatedAt | UpdatedAt
      sheet.appendRow([
        postData.BookName,
        postData.Author,
        postData.Category,
        postData.Language,
        postData.Description,
        postData.CoverImage,
        postData.TotalVolumes,
        postData.Featured ? "TRUE" : "FALSE",
        nowStr,
        nowStr
      ]);
      
      return outputJson({ success: true, message: "Book record saved successfully." });
    }
    
    // 3. EDIT BOOK
    if (action === "editBook") {
      var originalBookName = e.parameter.bookName;
      var sheet = ss.getSheetByName("Books");
      var dataRange = sheet.getDataRange();
      var values = dataRange.getValues();
      var nowStr = new Date().toISOString();
      var found = false;
      
      for (var i = 1; i < values.length; i++) {
        if (String(values[i][0]).trim().toLowerCase() === String(originalBookName).trim().toLowerCase()) {
          // Update values
          sheet.getRange(i + 1, 1).setValue(postData.BookName);
          sheet.getRange(i + 1, 2).setValue(postData.Author);
          sheet.getRange(i + 1, 3).setValue(postData.Category);
          sheet.getRange(i + 1, 4).setValue(postData.Language);
          sheet.getRange(i + 1, 5).setValue(postData.Description);
          sheet.getRange(i + 1, 6).setValue(postData.CoverImage);
          sheet.getRange(i + 1, 7).setValue(postData.TotalVolumes);
          sheet.getRange(i + 1, 8).setValue(postData.Featured ? "TRUE" : "FALSE");
          sheet.getRange(i + 1, 10).setValue(nowStr);
          found = true;
          break;
        }
      }
      
      if (found) {
        // Also Cascade update relationship keys in Volumes if BookName changed
        if (String(originalBookName).trim().toLowerCase() !== String(postData.BookName).trim().toLowerCase()) {
          var volSheet = ss.getSheetByName("Volumes");
          var volValues = volSheet.getDataRange().getValues();
          for (var j = 1; j < volValues.length; j++) {
            if (String(volValues[j][0]).trim().toLowerCase() === String(originalBookName).trim().toLowerCase()) {
              volSheet.getRange(j + 1, 1).setValue(postData.BookName);
            }
          }
        }
        return outputJson({ success: true, message: "Book and related volumes synchronized successfully." });
      }
      return outputJson({ success: false, message: "Original book not found inside Sheets: " + originalBookName });
    }
    
    // 4. DELETE BOOK
    if (action === "deleteBook") {
      var targetBookName = e.parameter.bookName;
      
      // Delete from Books Sheet
      var bookSheet = ss.getSheetByName("Books");
      var bValues = bookSheet.getDataRange().getValues();
      for (var i = bValues.length - 1; i >= 1; i--) {
        if (String(bValues[i][0]).trim().toLowerCase() === String(targetBookName).trim().toLowerCase()) {
          bookSheet.deleteRow(i + 1);
        }
      }
      
      // Cascade delete related Volumes
      var volSheet = ss.getSheetByName("Volumes");
      var vValues = volSheet.getDataRange().getValues();
      for (var j = vValues.length - 1; j >= 1; j--) {
        if (String(vValues[j][0]).trim().toLowerCase() === String(targetBookName).trim().toLowerCase()) {
          volSheet.deleteRow(j + 1);
        }
      }
      
      return outputJson({ success: true, message: "Book and all related volumes deleted." });
    }
    
    // 5. ADD VOLUME
    if (action === "addVolume") {
      var sheet = ss.getSheetByName("Volumes");
      var nowStr = new Date().toISOString();
      
      // BookName | VolumeNumber | VolumeName | Thumbnail | PDF | FileSize | CreatedAt
      sheet.appendRow([
        postData.BookName,
        postData.VolumeNumber,
        postData.VolumeName,
        postData.Thumbnail,
        postData.PDF,
        postData.FileSize,
        nowStr
      ]);
      
      return outputJson({ success: true, message: "Volume record added successfully." });
    }
    
    // 6. EDIT VOLUME
    if (action === "editVolume") {
      var bookName = e.parameter.bookName;
      var originalVolNumber = parseInt(e.parameter.volumeNumber);
      var sheet = ss.getSheetByName("Volumes");
      var values = sheet.getDataRange().getValues();
      var found = false;
      
      for (var i = 1; i < values.length; i++) {
        if (String(values[i][0]).trim().toLowerCase() === String(bookName).trim().toLowerCase() && 
            parseInt(values[i][1]) === originalVolNumber) {
          
          sheet.getRange(i + 1, 1).setValue(postData.BookName);
          sheet.getRange(i + 1, 2).setValue(postData.VolumeNumber);
          sheet.getRange(i + 1, 3).setValue(postData.VolumeName);
          sheet.getRange(i + 1, 4).setValue(postData.Thumbnail);
          sheet.getRange(i + 1, 5).setValue(postData.PDF);
          sheet.getRange(i + 1, 6).setValue(postData.FileSize);
          found = true;
          break;
        }
      }
      if (found) {
        return outputJson({ success: true, message: "Volume record updated." });
      }
      return outputJson({ success: false, message: "Target volume to edit not found." });
    }
    
    // 7. DELETE VOLUME
    if (action === "deleteVolume") {
      var bookName = e.parameter.bookName;
      var volNum = parseInt(e.parameter.volumeNumber);
      var sheet = ss.getSheetByName("Volumes");
      var values = sheet.getDataRange().getValues();
      var deleted = false;
      
      for (var i = values.length - 1; i >= 1; i--) {
        if (String(values[i][0]).trim().toLowerCase() === String(bookName).trim().toLowerCase() && 
            parseInt(values[i][1]) === volNum) {
          sheet.deleteRow(i + 1);
          deleted = true;
          break;
        }
      }
      if (deleted) {
        return outputJson({ success: true, message: "Volume deleted successfully." });
      }
      return outputJson({ success: false, message: "Volume entry not found." });
    }
    
    // 8. ADD CATEGORY
    if (action === "addCategory") {
      var sheet = ss.getSheetByName("Categories");
      if (!sheet) {
        sheet = ss.insertSheet("Categories");
      }
      if (sheet.getLastRow() === 0) {
        sheet.appendRow(["CategoryName"]);
      }
      sheet.appendRow([postData.CategoryName]);
      return outputJson({ success: true, message: "Category saved." });
    }
    
    // 9. DELETE CATEGORY
    if (action === "deleteCategory") {
      var catName = e.parameter.categoryName;
      var sheet = ss.getSheetByName("Categories");
      if (!sheet) {
        return outputJson({ success: true, message: "Category deleted (sheet did not exist)." });
      }
      var values = sheet.getDataRange().getValues();
      for (var i = values.length - 1; i >= 1; i--) {
        if (String(values[i][0]).trim().toLowerCase() === String(catName).trim().toLowerCase()) {
          sheet.deleteRow(i + 1);
        }
      }
      return outputJson({ success: true, message: "Category deleted." });
    }
    
    // 10. ADD LANGUAGE
    if (action === "addLanguage") {
      var sheet = ss.getSheetByName("Languages");
      if (!sheet) {
        sheet = ss.insertSheet("Languages");
      }
      if (sheet.getLastRow() === 0) {
        sheet.appendRow(["LanguageName"]);
      }
      sheet.appendRow([postData.LanguageName]);
      return outputJson({ success: true, message: "Language saved." });
    }
    
    // 11. DELETE LANGUAGE
    if (action === "deleteLanguage") {
      var langName = e.parameter.languageName;
      var sheet = ss.getSheetByName("Languages");
      if (!sheet) {
        return outputJson({ success: true, message: "Language deleted (sheet did not exist)." });
      }
      var values = sheet.getDataRange().getValues();
      for (var i = values.length - 1; i >= 1; i--) {
        if (String(values[i][0]).trim().toLowerCase() === String(langName).trim().toLowerCase()) {
          sheet.deleteRow(i + 1);
        }
      }
      return outputJson({ success: true, message: "Language deleted." });
    }
    
    // 12. ADD DARS BOOK
    if (action === "addDarsBook") {
      var sheet = ss.getSheetByName("DarsBooks");
      if (!sheet) {
        sheet = ss.insertSheet("DarsBooks");
      }
      if (sheet.getLastRow() === 0) {
        sheet.appendRow(["BookName", "Author", "DarsClass", "Language", "Description", "CoverImage", "TotalVolumes", "Featured", "CreatedAt", "UpdatedAt"]);
      }
      var nowStr = new Date().toISOString();
      
      sheet.appendRow([
        postData.BookName,
        postData.Author,
        postData.DarsClass,
        postData.Language,
        postData.Description,
        postData.CoverImage,
        postData.TotalVolumes,
        postData.Featured ? "TRUE" : "FALSE",
        nowStr,
        nowStr
      ]);
      
      return outputJson({ success: true, message: "Dars Book record saved successfully." });
    }
    
    // 13. EDIT DARS BOOK
    if (action === "editDarsBook") {
      var originalBookName = e.parameter.bookName;
      var sheet = ss.getSheetByName("DarsBooks");
      if (!sheet) {
        return outputJson({ success: false, message: "DarsBooks sheet does not exist." });
      }
      var dataRange = sheet.getDataRange();
      var values = dataRange.getValues();
      var nowStr = new Date().toISOString();
      var found = false;
      
      for (var i = 1; i < values.length; i++) {
        if (String(values[i][0]).trim().toLowerCase() === String(originalBookName).trim().toLowerCase()) {
          sheet.getRange(i + 1, 1).setValue(postData.BookName);
          sheet.getRange(i + 1, 2).setValue(postData.Author);
          sheet.getRange(i + 1, 3).setValue(postData.DarsClass);
          sheet.getRange(i + 1, 4).setValue(postData.Language);
          sheet.getRange(i + 1, 5).setValue(postData.Description);
          sheet.getRange(i + 1, 6).setValue(postData.CoverImage);
          sheet.getRange(i + 1, 7).setValue(postData.TotalVolumes);
          sheet.getRange(i + 1, 8).setValue(postData.Featured ? "TRUE" : "FALSE");
          sheet.getRange(i + 1, 10).setValue(nowStr);
          found = true;
          break;
        }
      }
      
      if (found) {
        return outputJson({ success: true, message: "Dars Book synchronized successfully." });
      }
      return outputJson({ success: false, message: "Original dars book not found inside Sheets: " + originalBookName });
    }
    
    // 14. DELETE DARS BOOK
    if (action === "deleteDarsBook") {
      var targetBookName = e.parameter.bookName;
      var bookSheet = ss.getSheetByName("DarsBooks");
      if (!bookSheet) {
        return outputJson({ success: true, message: "Dars Book deleted (sheet did not exist)." });
      }
      var bValues = bookSheet.getDataRange().getValues();
      for (var i = bValues.length - 1; i >= 1; i--) {
        if (String(bValues[i][0]).trim().toLowerCase() === String(targetBookName).trim().toLowerCase()) {
          bookSheet.deleteRow(i + 1);
        }
      }
      return outputJson({ success: true, message: "Dars Book deleted." });
    }
    
    // 15. ADD DARS CLASS
    if (action === "addDarsClass") {
      var sheet = ss.getSheetByName("DarsClasses");
      if (!sheet) {
        sheet = ss.insertSheet("DarsClasses");
      }
      if (sheet.getLastRow() === 0) {
        sheet.appendRow(["ClassName"]);
      }
      var clsNameVal = postData.ClassName || postData.className || postData.classname || "";
      sheet.appendRow([clsNameVal]);
      return outputJson({ success: true, message: "Dars Class saved." });
    }
    
    // 16. DELETE DARS CLASS
    if (action === "deleteDarsClass") {
      var clsName = e.parameter.className;
      var sheet = ss.getSheetByName("DarsClasses");
      if (!sheet) {
        return outputJson({ success: true, message: "Dars Class deleted (sheet did not exist)." });
      }
      var values = sheet.getDataRange().getValues();
      for (var i = values.length - 1; i >= 1; i--) {
        if (String(values[i][0]).trim().toLowerCase() === String(clsName).trim().toLowerCase()) {
          sheet.deleteRow(i + 1);
        }
      }
      return outputJson({ success: true, message: "Dars Class deleted." });
    }
    
    // 17. UPLOAD FILE TO GOOGLE DRIVE (IMAGES/PDFs)
    if (action === "uploadFile") {
      var base64Content = postData.fileBase64;
      var fileName = postData.fileName;
      var folderId = postData.folderId;
      
      var targetFolder;
      if (folderId && folderId !== "") {
        try {
          targetFolder = DriveApp.getFolderById(folderId);
        } catch (e) {
          // Fallback: search for or create a folder named after the type in user's root Drive
          var folderName = "Library_Uploads";
          if (folderId === "1Yw6wFYRkWqHuNMrtAMhZtWsaGs7BdkXi") folderName = "Library_Book_Covers";
          else if (folderId === "1cWY33VF2AVM3LC_8jLd8QPEJKcgQG73O") folderName = "Library_Volume_Thumbnails";
          else if (folderId === "1b8AtdXrxRNuHQFLbPCtFRVKRVT6t_rj8") folderName = "Library_Book_PDFs";
          
          var folders = DriveApp.getRootFolder().getFoldersByName(folderName);
          if (folders.hasNext()) {
            targetFolder = folders.next();
          } else {
            targetFolder = DriveApp.getRootFolder().createFolder(folderName);
          }
        }
      } else {
        // Fallback to drive root if empty
        targetFolder = DriveApp.getRootFolder();
      }
      
      // Convert base64 data to blob
      var fileBytes = Utilities.base64Decode(base64Content);
      var contentType = MimeType.PNG; // Default
      
      if (fileName.toLowerCase().endsWith(".pdf")) {
        contentType = MimeType.PDF;
      } else if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
        contentType = MimeType.JPEG;
      }
      
      var blob = Utilities.newBlob(fileBytes, contentType, fileName);
      var file = targetFolder.createFile(blob);
      
      // CRITICAL: Configure sharing settings to make link viewable publicly!
      file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
      
      // Generate Direct Download / Viewable link
      // For images, the lh3 d format is extremely reliable and accessible without Google login!
      var directUrl = "https://lh3.googleusercontent.com/d/" + file.getId();
      if (contentType === MimeType.PDF) {
        // For PDFs, we can also expose direct web view or direct download links
        directUrl = "https://drive.google.com/file/d/" + file.getId() + "/view?usp=sharing";
      }
      
      return outputJson({
        success: true,
        message: "File uploaded successfully to Google Drive folder.",
        url: directUrl
      });
    }
    
    return outputJson({ success: false, message: "Action '" + action + "' not recognized on POST." });
    
  } catch (error) {
    return outputJson({ success: false, message: "POST Exception: " + error.toString() });
  }
}
