package com.edupage.api.exceptions

/** Thrown when the provided username/password are incorrect. */
class BadCredentialsException(message: String = "Invalid username or password") : Exception(message)

/** Thrown when a CAPTCHA challenge is required to log in. */
class CaptchaException(message: String = "Captcha required") : Exception(message)

/** Thrown when the user is not logged in and a protected method is called. */
class NotLoggedInException(message: String = "You are not logged in") : Exception(message)

/** Thrown when expected data is missing from the Edupage response. */
class MissingDataException(message: String = "Missing data in response") : Exception(message)

/** Thrown when Edupage returns an error in its response payload. */
class RequestError(message: String) : Exception(message)

/** Thrown when the user lacks permissions for the requested operation. */
class InsufficientPermissionsException(message: String) : Exception(message)

/** Thrown when an unknown server error occurs. */
class UnknownServerError(message: String) : Exception(message)

/** Thrown when grade data cannot be parsed. */
class FailedToParseGradeDataException(message: String) : Exception(message)

/** Thrown when trying to use parent-only features on a non-parent account. */
class NotParentException(message: String = "This method can only be used on parent accounts") : Exception(message)

/** Thrown when the target of an online lesson check is not an online lesson. */
class NotAnOnlineLessonException(message: String = "This lesson is not an online lesson") : Exception(message)
