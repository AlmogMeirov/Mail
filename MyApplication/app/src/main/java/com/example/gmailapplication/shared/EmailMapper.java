// EmailMapper.java
package com.example.gmailapplication.shared;

import com.example.gmailapplication.database.entities.EmailEntity;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class EmailMapper {
    private static final Gson gson = new Gson();

    // Convert Email (network model) to EmailEntity (database model)
    public static EmailEntity toEntity(Email email) {
        if (email == null) return null;

        EmailEntity entity = new EmailEntity(email.id);
        entity.sender = email.sender;
        entity.recipient = email.recipient;
        entity.recipients = email.recipients;
        entity.subject = email.subject;
        entity.content = email.content;
        entity.timestamp = email.timestamp;
        entity.labels = email.labels;
        entity.groupId = email.groupId;
        entity.direction = email.direction != null ? email.direction : "received";
        entity.preview = email.preview;
        entity.needsSync = false;
        entity.syncStatus = "synced";

        // Handle otherParty conversion
        if (email.otherParty != null) {
            try {
                entity.otherPartyJson = gson.toJson(email.otherParty);
            } catch (Exception e) {
                entity.otherPartyJson = null;
            }
        }

        return entity;
    }

    // Convert EmailEntity (database model) to Email (UI model)
    public static Email toEmail(EmailEntity entity) {
        if (entity == null) return null;

        Email email = new Email();
        email.id = entity.id;
        email.sender = entity.sender;
        email.recipient = entity.recipient;
        email.recipients = entity.recipients;
        email.subject = entity.subject;
        email.content = entity.content;
        email.timestamp = entity.timestamp;
        email.labels = entity.labels;
        email.groupId = entity.groupId;
        email.direction = entity.direction;
        email.preview = entity.preview;

        // *** הוספת השדות החדשים ***
        email.isRead = entity.isRead;
        email.isStarred = entity.isStarred;
        email.isArchived = entity.isArchived;
        email.isDeleted = entity.isDeleted;

        // Handle otherParty conversion
        if (entity.otherPartyJson != null) {
            try {
                email.otherParty = gson.fromJson(entity.otherPartyJson, UserInfo.class);
            } catch (JsonSyntaxException e) {
                email.otherParty = null;
            }
        }

        return email;
    }
    // Convert SendEmailRequest to EmailEntity (for drafts)
    public static EmailEntity requestToEntity(SendEmailRequest request, String emailId) {
        if (request == null) return null;

        EmailEntity entity = new EmailEntity(emailId != null ? emailId : "draft_" + System.currentTimeMillis());
        entity.sender = request.sender;
        entity.recipient = request.recipient;
        entity.recipients = request.recipients;
        entity.subject = request.subject;
        entity.content = request.content;
        entity.direction = "sent";
        entity.isDraft = true;
        entity.needsSync = true;
        entity.syncStatus = "pending";

        // Convert string labels to Label objects
        if (request.labels != null) {
            entity.labels = request.labels.stream()
                    .map(Label::new)
                    .collect(Collectors.toList());
        }

        return entity;
    }

    // Convert EmailEntity to SendEmailRequest (for sending drafts)
    public static SendEmailRequest entityToRequest(EmailEntity entity) {
        if (entity == null) return null;

        SendEmailRequest request = new SendEmailRequest();
        request.sender = entity.sender;
        request.recipient = entity.recipient;
        request.recipients = entity.recipients;
        request.subject = entity.subject;
        request.content = entity.content;

        // Convert Label objects to string labels
        if (entity.labels != null) {
            request.labels = entity.labels.stream()
                    .map(label -> label.name)
                    .collect(Collectors.toList());
        } else {
            request.labels = new ArrayList<>();
            request.labels.add("inbox");
        }

        return request;
    }

    // Convert list of Email to list of EmailEntity
    public static List<EmailEntity> toEntities(List<Email> emails) {
        if (emails == null) return new ArrayList<>();

        return emails.stream()
                .map(EmailMapper::toEntity)
                .collect(Collectors.toList());
    }

    // Convert list of EmailEntity to list of Email
    public static List<Email> toEmails(List<EmailEntity> entities) {
        if (entities == null) return new ArrayList<>();

        return entities.stream()
                .map(EmailMapper::toEmail)
                .collect(Collectors.toList());
    }

    // Update existing EmailEntity from Email (for sync operations)
    public static void updateEntityFromEmail(EmailEntity entity, Email email) {
        if (entity == null || email == null) return;

        // Don't update ID - it should remain the same
        entity.sender = email.sender;
        entity.recipient = email.recipient;
        entity.recipients = email.recipients;
        entity.subject = email.subject;
        entity.content = email.content;
        entity.timestamp = email.timestamp;
        entity.labels = email.labels;
        entity.groupId = email.groupId;
        entity.direction = email.direction != null ? email.direction : entity.direction;
        entity.preview = email.preview;
        entity.lastModified = System.currentTimeMillis();

        // Update otherParty
        if (email.otherParty != null) {
            try {
                entity.otherPartyJson = gson.toJson(email.otherParty);
            } catch (Exception e) {
                entity.otherPartyJson = null;
            }
        }

        // If this was a draft that got sent, convert it
        if (entity.isDraft && "sent".equals(email.direction)) {
            entity.isDraft = false;
            entity.draftId = null;
        }

        // Mark as synced if it came from server
        entity.needsSync = false;
        entity.syncStatus = "synced";
    }

    // Create a copy of EmailEntity with updated fields
    public static EmailEntity copyEntity(EmailEntity original) {
        if (original == null) return null;

        EmailEntity copy = new EmailEntity(original.id);
        copy.sender = original.sender;
        copy.recipient = original.recipient;
        copy.recipients = original.recipients;
        copy.subject = original.subject;
        copy.content = original.content;
        copy.timestamp = original.timestamp;
        copy.labels = original.labels;
        copy.groupId = original.groupId;
        copy.direction = original.direction;
        copy.preview = original.preview;
        copy.isRead = original.isRead;
        copy.isStarred = original.isStarred;
        copy.isArchived = original.isArchived;
        copy.isDeleted = original.isDeleted;
        copy.localTimestamp = original.localTimestamp;
        copy.lastModified = original.lastModified;
        copy.needsSync = original.needsSync;
        copy.syncStatus = original.syncStatus;
        copy.isDraft = original.isDraft;
        copy.draftId = original.draftId;
        copy.otherPartyJson = original.otherPartyJson;

        return copy;
    }

    // Helper method to create preview text from content
    public static String generatePreview(String content, int maxLength) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        String cleanContent = content.replaceAll("<[^>]+>", "").trim(); // Remove HTML tags

        if (cleanContent.length() <= maxLength) {
            return cleanContent;
        }

        return cleanContent.substring(0, maxLength) + "...";
    }

    // Helper method to determine email direction based on current user
    public static String determineDirection(Email email, String currentUserEmail) {
        if (email.direction != null && !email.direction.isEmpty()) {
            return email.direction;
        }

        if (currentUserEmail != null && currentUserEmail.equals(email.sender)) {
            return "sent";
        } else {
            return "received";
        }
    }


    // Private constructor to prevent instantiation
    private EmailMapper() {}
}