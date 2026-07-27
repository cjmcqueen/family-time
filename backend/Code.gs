var DAILY_API_KEY = PropertiesService.getScriptProperties().getProperty("DAILY_API_KEY");
var SHEET_ID = PropertiesService.getScriptProperties().getProperty("SHEET_ID");
var SHEET_NAME = "Contacts"; // Assumes column A: Name, Column B: Email

// 1. Fetch Contacts for the Portal TV UI
function doGet(e) {
  var sheet = SpreadsheetApp.openById(SHEET_ID).getSheetByName(SHEET_NAME);
  if (!sheet) {
    return ContentService.createTextOutput(JSON.stringify({ error: "Sheet not found" }))
                         .setMimeType(ContentService.MimeType.JSON);
  }
  
  var data = sheet.getDataRange().getValues();
  var contacts = [];
  
  // Skip header row (index 0)
  for (var i = 1; i < data.length; i++) {
    if (data[i][0] && data[i][1]) {
      contacts.push({ name: data[i][0], email: data[i][1] });
    }
  }
  
  return ContentService.createTextOutput(JSON.stringify(contacts))
                       .setMimeType(ContentService.MimeType.JSON);
}

// 2. Create Room & Send Invites
function doPost(e) {
  try {
    var payload = JSON.parse(e.postData.contents);
    var selectedEmails = payload.emails || [];
    
    // Create a Daily room via REST API
    var response = UrlFetchApp.fetch("https://api.daily.co/v1/rooms", {
      method: "post",
      headers: { "Authorization": "Bearer " + DAILY_API_KEY },
      contentType: "application/json",
      payload: JSON.stringify({ properties: { exp: Math.floor(Date.now() / 1000) + 3600 } }) // expires in 1 hour
    });
    
    var roomUrl = JSON.parse(response.getContentText()).url;
    
    // Email the selected family members
    for (var i = 0; i < selectedEmails.length; i++) {
      MailApp.sendEmail(
        selectedEmails[i], 
        "Join Family Time!", 
        "A video chat is starting! Join right in your browser here: " + roomUrl
      );
    }
    
    return ContentService.createTextOutput(JSON.stringify({ url: roomUrl }))
                         .setMimeType(ContentService.MimeType.JSON);
  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({ error: error.message }))
                         .setMimeType(ContentService.MimeType.JSON);
  }
}

// 3. Automated Daily Health Check
// Bind this to a Time-Driven Trigger (e.g., Daily between 8 AM - 9 AM)
function dailyHealthCheck() {
  try {
    // Attempt to create a test room
    var createResponse = UrlFetchApp.fetch("https://api.daily.co/v1/rooms", {
      method: "post",
      headers: { "Authorization": "Bearer " + DAILY_API_KEY },
      contentType: "application/json",
      payload: JSON.stringify({ properties: { exp: Math.floor(Date.now() / 1000) + 300 } }) // 5 min expiry
    });
    
    var responseData = JSON.parse(createResponse.getContentText());
    
    if (createResponse.getResponseCode() === 200 && responseData.name) {
      // Success! Clean up by deleting the test room
      UrlFetchApp.fetch("https://api.daily.co/v1/rooms/" + responseData.name, {
        method: "delete",
        headers: { "Authorization": "Bearer " + DAILY_API_KEY }
      });
      Logger.log("Health check passed.");
    } else {
      throw new Error("Invalid response from Daily API.");
    }
  } catch (error) {
    // Alert the developer if billing failed, API key rotated, or service is down
    MailApp.sendEmail(
      Session.getEffectiveUser().getEmail(),
      "URGENT: Family Time Health Check Failed",
      "The daily health check for Family Time failed. This could indicate a billing issue or API outage with Daily.co.\n\nError: " + error.message
    );
  }
}
