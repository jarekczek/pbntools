package jc.wwwserver

class WwwSession(val id: String) {
  var lastPage: String? = null
  var authenticated = false
}