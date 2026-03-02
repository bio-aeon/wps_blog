package su.wps.blog.validation

import cats.data.Validated
import org.specs2.mutable.Specification
import su.wps.blog.tools.ValidatedMatchers

class ValidationSpec extends Specification with ValidatedMatchers {

  "Validation.validateLimit" >> {
    "accepts valid limit within range" >> {
      Validation.validateLimit(10) must beValid(10)
    }

    "accepts minimum limit" >> {
      Validation.validateLimit(Validation.MinLimit) must beValid(Validation.MinLimit)
    }

    "accepts maximum limit" >> {
      Validation.validateLimit(Validation.MaxLimit) must beValid(Validation.MaxLimit)
    }

    "rejects limit below minimum" >> {
      Validation.validateLimit(0) must beInvalid
    }

    "rejects negative limit" >> {
      Validation.validateLimit(-5) must beInvalid
    }

    "rejects limit above maximum" >> {
      Validation.validateLimit(101) must beInvalid
    }
  }

  "Validation.validateOffset" >> {
    "accepts zero offset" >> {
      Validation.validateOffset(0) must beValid(0)
    }

    "accepts positive offset" >> {
      Validation.validateOffset(50) must beValid(50)
    }

    "rejects negative offset" >> {
      Validation.validateOffset(-1) must beInvalid
    }
  }

  "Validation.validatePagination" >> {
    "accepts valid limit and offset" >> {
      Validation.validatePagination(10, 0) must beValid((10, 0))
    }

    "rejects invalid limit with valid offset" >> {
      Validation.validatePagination(0, 0) must beInvalid
    }

    "rejects valid limit with invalid offset" >> {
      Validation.validatePagination(10, -1) must beInvalid
    }

    "accumulates errors for both invalid limit and offset" >> {
      Validation.validatePagination(0, -1) match {
        case Validated.Invalid(errors) => errors.length mustEqual 2
        case _ => ko("expected invalid")
      }
    }
  }

  "Validation.validateCommentName" >> {
    "accepts valid name" >> {
      Validation.validateCommentName("Author") must beValid("Author")
    }

    "trims whitespace from name" >> {
      Validation.validateCommentName("  Author  ") must beValid("Author")
    }

    "rejects empty name" >> {
      Validation.validateCommentName("") must beInvalid
    }

    "rejects whitespace-only name" >> {
      Validation.validateCommentName("   ") must beInvalid
    }

    "rejects name exceeding max length" >> {
      val longName = "a" * (Validation.MaxCommentNameLength + 1)
      Validation.validateCommentName(longName) must beInvalid
    }
  }

  "Validation.validateCommentEmail" >> {
    "accepts valid email" >> {
      Validation.validateCommentEmail("test@example.com") must beValid("test@example.com")
    }

    "trims whitespace from email" >> {
      Validation.validateCommentEmail("  test@example.com  ") must beValid("test@example.com")
    }

    "rejects empty email" >> {
      Validation.validateCommentEmail("") must beInvalid
    }

    "rejects invalid email format" >> {
      Validation.validateCommentEmail("not-an-email") must beInvalid
    }

    "rejects email without domain" >> {
      Validation.validateCommentEmail("user@") must beInvalid
    }

    "rejects email exceeding max length" >> {
      val longEmail = "a" * 250 + "@b.com"
      Validation.validateCommentEmail(longEmail) must beInvalid
    }
  }

  "Validation.validateCommentText" >> {
    "accepts valid text" >> {
      Validation.validateCommentText("Hello world") must beValid("Hello world")
    }

    "trims whitespace from text" >> {
      Validation.validateCommentText("  Hello  ") must beValid("Hello")
    }

    "rejects empty text" >> {
      Validation.validateCommentText("") must beInvalid
    }

    "rejects whitespace-only text" >> {
      Validation.validateCommentText("   ") must beInvalid
    }

    "rejects text exceeding max length" >> {
      val longText = "a" * (Validation.MaxCommentTextLength + 1)
      Validation.validateCommentText(longText) must beInvalid
    }

    "sanitizes HTML in valid text" >> {
      Validation.validateCommentText("<script>alert('xss')</script>") must beValid(
        "&lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;"
      )
    }
  }

  "Validation.validateComment" >> {
    "accepts valid comment fields" >> {
      Validation.validateComment("Author", "test@example.com", "Comment text") must beValid(
        ("Author", "test@example.com", "Comment text")
      )
    }

    "accumulates errors for multiple invalid fields" >> {
      Validation.validateComment("", "invalid", "") match {
        case Validated.Invalid(errors) => errors.length mustEqual 3
        case _ => ko("expected invalid")
      }
    }
  }

  "Validation.validateContactName" >> {
    "accepts valid name" >> {
      Validation.validateContactName("John") must beValid("John")
    }

    "trims whitespace" >> {
      Validation.validateContactName("  John  ") must beValid("John")
    }

    "rejects empty name" >> {
      Validation.validateContactName("") must beInvalid
    }

    "rejects name exceeding max length" >> {
      val longName = "a" * (Validation.MaxContactNameLength + 1)
      Validation.validateContactName(longName) must beInvalid
    }
  }

  "Validation.validateContactEmail" >> {
    "accepts valid email" >> {
      Validation.validateContactEmail("test@example.com") must beValid("test@example.com")
    }

    "rejects invalid email format" >> {
      Validation.validateContactEmail("not-an-email") must beInvalid
    }

    "rejects email exceeding max length" >> {
      val longEmail = "a" * 250 + "@b.com"
      Validation.validateContactEmail(longEmail) must beInvalid
    }

    "rejects empty email" >> {
      Validation.validateContactEmail("") must beInvalid
    }
  }

  "Validation.validateContactSubject" >> {
    "accepts valid subject" >> {
      Validation.validateContactSubject("Hello there") must beValid("Hello there")
    }

    "rejects subject too short" >> {
      Validation.validateContactSubject("ab") must beInvalid
    }

    "rejects subject too long" >> {
      val longSubject = "a" * (Validation.MaxContactSubjectLength + 1)
      Validation.validateContactSubject(longSubject) must beInvalid
    }

    "rejects empty subject" >> {
      Validation.validateContactSubject("") must beInvalid
    }
  }

  "Validation.validateContactMessage" >> {
    "accepts valid message" >> {
      Validation.validateContactMessage("Hello, this is a test message") must beValid(
        "Hello, this is a test message"
      )
    }

    "rejects message too short" >> {
      Validation.validateContactMessage("short") must beInvalid
    }

    "rejects message too long" >> {
      val longMessage = "a" * (Validation.MaxContactMessageLength + 1)
      Validation.validateContactMessage(longMessage) must beInvalid
    }

    "sanitizes HTML in message" >> {
      Validation.validateContactMessage("<script>alert('xss')</script>long enough") must beValid(
        "&lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;long enough"
      )
    }
  }

  "Validation.validateContact" >> {
    "accepts valid contact fields" >> {
      Validation.validateContact(
        "John",
        "john@example.com",
        "Subject here",
        "Message body text"
      ) must beValid(("John", "john@example.com", "Subject here", "Message body text"))
    }

    "accumulates errors for multiple invalid fields" >> {
      Validation.validateContact("", "invalid", "ab", "short") match {
        case Validated.Invalid(errors) => errors.length mustEqual 4
        case _ => ko("expected invalid")
      }
    }
  }

  "Validation.sanitizeHtml" >> {
    "escapes angle brackets" >> {
      Validation.sanitizeHtml("<div>") mustEqual "&lt;div&gt;"
    }

    "escapes ampersands" >> {
      Validation.sanitizeHtml("a & b") mustEqual "a &amp; b"
    }

    "escapes double quotes" >> {
      Validation.sanitizeHtml("""say "hello"""") mustEqual "say &quot;hello&quot;"
    }

    "escapes single quotes" >> {
      Validation.sanitizeHtml("it's") mustEqual "it&#x27;s"
    }

    "escapes script tags" >> {
      Validation.sanitizeHtml("<script>alert('xss')</script>") mustEqual
        "&lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;"
    }

    "passes through plain text unchanged" >> {
      Validation.sanitizeHtml("Hello world") mustEqual "Hello world"
    }
  }
}
