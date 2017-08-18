package cn.weijie.asgard

object MIME {
    const val ALL = "*/*"
    const val TEXT_ALL = "text/*"
    const val APPLICATION_ALL = "application/*"
    const val TEXT_PLAIN = "text/plain"
    const val TEXT_HTML = "text/html"
    const val APPLICATION_OCTET_STREAM = "application/octet-stream"
    const val APPLICATION_PDF = "application/pdf"
    const val TEXT_CSS = "text/css"
    const val APPLICATION_JAVASCRIPT = "application/javascript"
    const val APPLICATION_JSON = "application/json"
    const val MULTIPART_FORM_DATA = "multipart/form-data"
    const val APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded"
}

object REQUEST_FIELD {
    const val INPUT = "input"
    const val HEADERS = "headers"
    const val COOKIES = "cookies"
    const val PARAMS = "params"
    const val URI = "uri"
    const val QUERY = "query"
    const val HOST = "host"
    const val PATH = "path"
    const val FILE_STREAM = "fileStream"
}

object RESPONSE_FIELD {
    const val CONTENT_TYPE = "content-type"
    const val TEMPLATE = "x_template"
}