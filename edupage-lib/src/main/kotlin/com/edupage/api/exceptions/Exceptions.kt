package com.edupage.api.exceptions

class BadCredentialsException(message: String = "Invalid username or password") : Exception(message)

class CaptchaException(message: String = "Captcha required") : Exception(message)

class NotLoggedInException(message: String = "You are not logged in") : Exception(message)

class MissingDataException(message: String = "Missing data in response") : Exception(message)

class RequestError(message: String) : Exception(message)

class InsufficientPermissionsException(message: String) : Exception(message)

class UnknownServerError(message: String) : Exception(message)

class FailedToParseGradeDataException(message: String) : Exception(message)

class NotParentException(message: String = "This method can only be used on parent accounts") : Exception(message)

class NotAnOnlineLessonException(message: String = "This lesson is not an online lesson") : Exception(message)
