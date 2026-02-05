package su.wps.blog.validation

import cats.data.Validated
import org.specs2.mutable.Specification
import su.wps.blog.tools.ValidatedMatchers

class ValidationSpec extends Specification with ValidatedMatchers {

  "Validation.validateLimit" >> {
    "accept valid limit within range" >> {
      Validation.validateLimit(10) must beValid(10)
    }

    "accept minimum limit" >> {
      Validation.validateLimit(Validation.MinLimit) must beValid(Validation.MinLimit)
    }

    "accept maximum limit" >> {
      Validation.validateLimit(Validation.MaxLimit) must beValid(Validation.MaxLimit)
    }

    "reject limit below minimum" >> {
      Validation.validateLimit(0) must beInvalid
    }

    "reject negative limit" >> {
      Validation.validateLimit(-5) must beInvalid
    }

    "reject limit above maximum" >> {
      Validation.validateLimit(101) must beInvalid
    }
  }

  "Validation.validateOffset" >> {
    "accept zero offset" >> {
      Validation.validateOffset(0) must beValid(0)
    }

    "accept positive offset" >> {
      Validation.validateOffset(50) must beValid(50)
    }

    "reject negative offset" >> {
      Validation.validateOffset(-1) must beInvalid
    }
  }

  "Validation.validatePagination" >> {
    "accept valid limit and offset" >> {
      Validation.validatePagination(10, 0) must beValid((10, 0))
    }

    "reject invalid limit with valid offset" >> {
      Validation.validatePagination(0, 0) must beInvalid
    }

    "reject valid limit with invalid offset" >> {
      Validation.validatePagination(10, -1) must beInvalid
    }

    "accumulate errors for both invalid limit and offset" >> {
      Validation.validatePagination(0, -1) match {
        case Validated.Invalid(errors) => errors.length mustEqual 2
        case _                         => ko("expected invalid")
      }
    }
  }

  "Validation.validateCommentName" >> {
    "accept valid name" >> {
      Validation.validateCommentName("Author") must beValid("Author")
    }

    "trim whitespace from name" >> {
      Validation.validateCommentName("  Author  ") must beValid("Author")
    }

    "reject empty name" >> {
      Validation.validateCommentName("") must beInvalid
    }

    "reject whitespace-only name" >> {
      Validation.validateCommentName("   ") must beInvalid
    }

    "reject name exceeding max length" >> {
      val longName = "a" * (Validation.MaxCommentNameLength + 1)
      Validation.validateCommentName(longName) must beInvalid
    }
  }

  "Validation.validateCommentEmail" >> {
    "accept valid email" >> {
      Validation.validateCommentEmail("test@example.com") must beValid("test@example.com")
    }

    "trim whitespace from email" >> {
      Validation.validateCommentEmail("  test@example.com  ") must beValid("test@example.com")
    }

    "reject empty email" >> {
      Validation.validateCommentEmail("") must beInvalid
    }

    "reject invalid email format" >> {
      Validation.validateCommentEmail("not-an-email") must beInvalid
    }

    "reject email without domain" >> {
      Validation.validateCommentEmail("user@") must beInvalid
    }

    "reject email exceeding max length" >> {
      val longEmail = "a" * 250 + "@b.com"
      Validation.validateCommentEmail(longEmail) must beInvalid
    }
  }

  "Validation.validateCommentText" >> {
    "accept valid text" >> {
      Validation.validateCommentText("Hello world") must beValid("Hello world")
    }

    "trim whitespace from text" >> {
      Validation.validateCommentText("  Hello  ") must beValid("Hello")
    }

    "reject empty text" >> {
      Validation.validateCommentText("") must beInvalid
    }

    "reject whitespace-only text" >> {
      Validation.validateCommentText("   ") must beInvalid
    }

    "reject text exceeding max length" >> {
      val longText = "a" * (Validation.MaxCommentTextLength + 1)
      Validation.validateCommentText(longText) must beInvalid
    }

    "sanitize HTML in valid text" >> {
      Validation.validateCommentText("<script>alert('xss')</script>") must beValid(
        "&lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;"
      )
    }
  }

  "Validation.validateComment" >> {
    "accept valid comment fields" >> {
      Validation.validateComment("Author", "test@example.com", "Comment text") must beValid(
        ("Author", "test@example.com", "Comment text")
      )
    }

    "accumulate errors for multiple invalid fields" >> {
      Validation.validateComment("", "invalid", "") match {
        case Validated.Invalid(errors) => errors.length mustEqual 3
        case _                         => ko("expected invalid")
      }
    }
  }

  "Validation.sanitizeHtml" >> {
    "escape angle brackets" >> {
      Validation.sanitizeHtml("<div>") mustEqual "&lt;div&gt;"
    }

    "escape ampersands" >> {
      Validation.sanitizeHtml("a & b") mustEqual "a &amp; b"
    }

    "escape double quotes" >> {
      Validation.sanitizeHtml("""say "hello"""") mustEqual "say &quot;hello&quot;"
    }

    "escape single quotes" >> {
      Validation.sanitizeHtml("it's") mustEqual "it&#x27;s"
    }

    "escape script tags" >> {
      Validation.sanitizeHtml("<script>alert('xss')</script>") mustEqual
        "&lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;"
    }

    "pass through plain text unchanged" >> {
      Validation.sanitizeHtml("Hello world") mustEqual "Hello world"
    }
  }
}
