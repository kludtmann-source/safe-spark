package com.example.safespark.ml

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit-Tests für ConversationBuffer
 * 
 * Testet das per-Contact Buffer System:
 * - Separate Buffers pro Chat-Titel
 * - Contact-ID Generierung
 * - Buffer Isolation zwischen verschiedenen Kontakten
 */
class ConversationBufferTest {

    @Before
    fun setUp() {
        // Clean slate für jeden Test
        ConversationBuffer.clearAll()
    }

    @After
    fun tearDown() {
        ConversationBuffer.clearAll()
    }

    @Test
    fun `generateContactId creates different IDs for different chat titles`() {
        // Arrange
        val packageName = "com.whatsapp"
        val chatTitle1 = "Oma"
        val chatTitle2 = "Unknown Stranger"
        
        // Act
        val contactId1 = ConversationBuffer.generateContactId(packageName, chatTitle1)
        val contactId2 = ConversationBuffer.generateContactId(packageName, chatTitle2)
        
        // Assert
        assertThat(contactId1).isNotEqualTo(contactId2)
        assertThat(contactId1).startsWith("C")
        assertThat(contactId2).startsWith("C")
    }

    @Test
    fun `generateContactId creates same ID for same chat title`() {
        // Arrange
        val packageName = "com.whatsapp"
        val chatTitle = "Max"
        
        // Act
        val contactId1 = ConversationBuffer.generateContactId(packageName, chatTitle)
        val contactId2 = ConversationBuffer.generateContactId(packageName, chatTitle)
        
        // Assert
        assertThat(contactId1).isEqualTo(contactId2)
    }

    @Test
    fun `generateContactId creates different IDs for same name in different apps`() {
        // Arrange
        val chatTitle = "Max"
        
        // Act
        val whatsappId = ConversationBuffer.generateContactId("com.whatsapp", chatTitle)
        val telegramId = ConversationBuffer.generateContactId("org.telegram", chatTitle)
        
        // Assert
        assertThat(whatsappId).isNotEqualTo(telegramId)
    }

    @Test
    fun `messages are stored in separate buffers per contact`() {
        // Arrange
        val packageName = "com.whatsapp"
        val contactId1 = ConversationBuffer.generateContactId(packageName, "Oma")
        val contactId2 = ConversationBuffer.generateContactId(packageName, "Stranger")
        
        val message1 = ConversationBuffer.ConversationMessage(
            text = "Hello from Oma",
            authorId = contactId1,
            timestamp = System.currentTimeMillis(),
            isLocalUser = false
        )
        
        val message2 = ConversationBuffer.ConversationMessage(
            text = "Hello from Stranger",
            authorId = contactId2,
            timestamp = System.currentTimeMillis(),
            isLocalUser = false
        )
        
        // Act
        ConversationBuffer.addMessage(contactId1, message1)
        ConversationBuffer.addMessage(contactId2, message2)
        
        val conversation1 = ConversationBuffer.getConversation(contactId1)
        val conversation2 = ConversationBuffer.getConversation(contactId2)
        
        // Assert
        assertThat(conversation1).hasSize(1)
        assertThat(conversation2).hasSize(1)
        assertThat(conversation1[0].text).isEqualTo("Hello from Oma")
        assertThat(conversation2[0].text).isEqualTo("Hello from Stranger")
    }

    @Test
    fun `buffer correctly tracks multiple messages per contact`() {
        // Arrange
        val packageName = "com.whatsapp"
        val contactId = ConversationBuffer.generateContactId(packageName, "Max")
        
        // Act
        for (i in 1..5) {
            val message = ConversationBuffer.ConversationMessage(
                text = "Message $i",
                authorId = contactId,
                timestamp = System.currentTimeMillis(),
                isLocalUser = false
            )
            ConversationBuffer.addMessage(contactId, message)
        }
        
        val conversation = ConversationBuffer.getConversation(contactId)
        
        // Assert
        assertThat(conversation).hasSize(5)
        assertThat(conversation[0].text).isEqualTo("Message 1")
        assertThat(conversation[4].text).isEqualTo("Message 5")
    }

    @Test
    fun `getActiveConversationCount returns correct number of active conversations`() {
        // Arrange
        val packageName = "com.whatsapp"
        val contactId1 = ConversationBuffer.generateContactId(packageName, "Contact1")
        val contactId2 = ConversationBuffer.generateContactId(packageName, "Contact2")
        val contactId3 = ConversationBuffer.generateContactId(packageName, "Contact3")
        
        val message = ConversationBuffer.ConversationMessage(
            text = "Test",
            authorId = "test",
            timestamp = System.currentTimeMillis(),
            isLocalUser = false
        )
        
        // Act
        ConversationBuffer.addMessage(contactId1, message)
        ConversationBuffer.addMessage(contactId2, message)
        ConversationBuffer.addMessage(contactId3, message)
        
        // Assert
        assertThat(ConversationBuffer.getActiveConversationCount()).isEqualTo(3)
    }

    @Test
    fun `clearConversation removes only specific contact buffer`() {
        // Arrange
        val packageName = "com.whatsapp"
        val contactId1 = ConversationBuffer.generateContactId(packageName, "Contact1")
        val contactId2 = ConversationBuffer.generateContactId(packageName, "Contact2")
        
        val message = ConversationBuffer.ConversationMessage(
            text = "Test",
            authorId = "test",
            timestamp = System.currentTimeMillis(),
            isLocalUser = false
        )
        
        ConversationBuffer.addMessage(contactId1, message)
        ConversationBuffer.addMessage(contactId2, message)
        
        // Act
        ConversationBuffer.clearConversation(contactId1)
        
        // Assert
        assertThat(ConversationBuffer.hasConversation(contactId1)).isFalse()
        assertThat(ConversationBuffer.hasConversation(contactId2)).isTrue()
    }

    @Test
    fun `messages with packageName fallback go to same buffer`() {
        // Arrange: Simulate fallback scenario where chat title is not available
        val packageName = "com.whatsapp"
        val contactIdWithFallback = ConversationBuffer.generateContactId(packageName, packageName)
        
        val message1 = ConversationBuffer.ConversationMessage(
            text = "Message without chat title 1",
            authorId = contactIdWithFallback,
            timestamp = System.currentTimeMillis(),
            isLocalUser = false
        )
        
        val message2 = ConversationBuffer.ConversationMessage(
            text = "Message without chat title 2",
            authorId = contactIdWithFallback,
            timestamp = System.currentTimeMillis(),
            isLocalUser = false
        )
        
        // Act
        ConversationBuffer.addMessage(contactIdWithFallback, message1)
        ConversationBuffer.addMessage(contactIdWithFallback, message2)
        
        val conversation = ConversationBuffer.getConversation(contactIdWithFallback)
        
        // Assert
        assertThat(conversation).hasSize(2)
    }

    @Test
    fun `Osprey input format includes proper tags`() {
        // Arrange
        val packageName = "com.whatsapp"
        val contactId = ConversationBuffer.generateContactId(packageName, "TestContact")
        
        val childMessage = ConversationBuffer.ConversationMessage(
            text = "Hi there",
            authorId = "child",
            timestamp = System.currentTimeMillis(),
            isLocalUser = true
        )
        
        val contactMessage = ConversationBuffer.ConversationMessage(
            text = "Hello child",
            authorId = contactId,
            timestamp = System.currentTimeMillis() + 1000,
            isLocalUser = false
        )
        
        // Act
        ConversationBuffer.addMessage(contactId, childMessage)
        ConversationBuffer.addMessage(contactId, contactMessage)
        
        val ospreyInput = ConversationBuffer.getOspreyInput(contactId)
        
        // Assert
        assertThat(ospreyInput).contains("[CHILD]")
        assertThat(ospreyInput).contains("[CONTACT]")
        assertThat(ospreyInput).contains("[SEP]")
        assertThat(ospreyInput).contains("Hi there")
        assertThat(ospreyInput).contains("Hello child")
    }
}
