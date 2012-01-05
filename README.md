#README

Welcome to the send-mail Clojure library wiki.

## Introduction

send-mail is a simple Clojure library for sending emails, written in pure Clojure, and licensed under the [Eclipse Public License](https://github.com/dbleyl/send-mail/blob/master/epl-v10.html).  The original use case was for sending emails from a noir web app hosted on Heroku. Inspired by [this tutorial on Heroku](http://blog.heroku.com/archives/2009/11/9/tech_sending_email_with_gmail/) for sending mail from rails apps, and the referenced article on sending email using gmail.

## Features

* Minimalist
* No underlying dependencies
* Gmail support 
* STARTTLS support via secure sockets

## Getting Started

### Setup

You'll need a gmail account.

### Usage
1. 'use' or 'require' send-mail in your namespace.

>`(ns your-namespace (:use send-mail)...) `

2. Call one of the send-mail functions (multiple recipients can be defined in a collection of strings):

>  `(send-mail "from-email" "to-email" "subject" "message" "from-gmail-passwd")`

> `(send-mail "from-email" "to-email" "subject" "message" "gmail-acct" "gmail-password")`
   
> `(send-mail "from-email" "to-email" "subject" "message" "host" "port" "server-acct" "server-port" timeout-in-millis)`

Examples:

`(send-mail "your-from@gmail.com" "destination@localhost" "Website Inquiry"
              (join \newline (map (fn [[k v]] (format "%s = %s" (name k) v) ) contact))
                        "s3cr3t")`

`(send-mail "your-from@gmail.com" ["dest1@localhost" "dest2@localhost"] "Website Inquiry"
              (join \newline (map (fn [[k v]] (format "%s = %s" (name k) v) ) contact))
                        "s3cr3t")`
## Known Issues & Limitations

The library has been designed for use with gmail initially.  It may or may not work with other smtp servers.  It is not a full implementation of SMTP, although it uses SMTP.

* Assumes the smtp server supports STARTTLS.
* Assumes the smtp server supports AUTH LOGIN.
* Gmail limits the number of emails to 500 per day (according to the article referenced in the introduction).
* There is little to no error handling.
* The SMTP defines timeouts in terms of minutes; it's important to set a reasonable timeout or use the default. The function without a timeout defaults to 1 minute.
* Attachments aren't supported.
* Aliases aren't supported.
* Logs with printlns.
* Uses a Java String to represent the password. Using a String is considered less secure than using a char array due to jvm internals.

## Alternatives

* javax.mail
* [Apache Commons Mail](http://commons.apache.org/email/)
* [Postal](https://github.com/drewr/postal) (wraps javax.mail)
