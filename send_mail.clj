;   Copyright (c) Donald Bleyl. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns send-mail
  ^{:doc "Utility for sending emails through gmail using an existing gmail account."}
  (:require [clojure.java.io :as io])
  (:require [base64 :as b64])
  (:import java.net.Socket)
  (:import [javax.net.ssl SSLSocketFactory SSLSocket]))

(defn- last-line?
  "Checks whether the line contains a '-' at the 4th position, indicating there are more lines remaining in the response."
         [line]
         (not (= \- (.charAt line 3))))

(defn- read-lines
  "Reads response lines from the server, using SMTP's 'has-next-line' indicator to terminate. Returns the last line."
  [rdr]
  (loop [line (.readLine rdr)]
    (println line)
    (if (not (last-line? line))
      (recur (.readLine rdr)))))

(defn- send-command
  "Sends a command to the server, adding carriage return/linefeed, flushing, and reading the response."
  [rdr wtr cmd]
  (println cmd)
  (.write wtr cmd)
  (.write wtr "\r\n")
  (.flush wtr)
  (read-lines rdr))

(defn- init-secure-session
  "Send the STARTTLS command to initiate the secure communication session."
  [rdr wtr]
  (send-command rdr wtr "STARTTLS"))

(defn- ehlo
  "Invokes EHLO command, and processes response."
  [rdr wtr host]
  (send-command rdr wtr (str "EHLO " host)))

(defn- auth
  "Sends the user string to the server, returns the response string."
  [rdr wtr user passwd]
  (send-command rdr wtr "AUTH LOGIN")
  (send-command rdr wtr (b64/encode user))
  (send-command rdr wtr (b64/encode passwd)))

(defn- add-from
  "Sends the 'MAIL FROM' command; returns the response string."
  [rdr wtr from-address]
  (send-command rdr wtr (str "MAIL FROM:<" from-address ">")))

(defn- add-to
  "Sends the 'RCPT TO' command; returns the server response as string."
  [rdr wtr to-address]
  (send-command rdr wtr (str "RCPT TO:<" to-address ">")))

(defn- add-all-to
  "Takes a collection of email addresses, sending a 'RCPT TO' command to each, returning a map of the status for each add."
  [rdr wtr addresses]
  (loop [addr (first addresses) [i & more] (next addresses)]
    (add-to rdr wtr addr)
    (if i
      (recur i more))))

(defn- start-message
  "initiates the message by sending the DATA command; returns the server response as a string."
  [rdr wtr]
  (send-command rdr wtr "DATA"))

(defn- write-message-header
  "takes a reader, writer, from-address, to-address and subject, does not
  return a value."
  [wtr from-address to-address subject]
  (.write wtr (str "From: <" from-address ">\r\n"))
  (if (string? to-address)
    (.write wtr (str "To: <" to-address ">\r\n"))
    (do
      (.write wtr (str "To: <" (first to-address) ">\r\n"))
      (loop [addr (second to-address) [i & addrs] (next (next to-address))]
        (.write wtr (str "Cc: <" addr ">\r\n"))
        (if i
          (recur i (next addrs))))))
  (.write wtr
          (str "Date: " 
               (.format 
                 (java.text.SimpleDateFormat. "EEE, d MMM yyyy HH:mm:ss Z") 
                 (java.util.Date.))
               "\r\n"))
  (.write wtr (str "Subject: " subject "\r\n")))

(defn- escape-msg
  "Replaces the message body termination indicator '\r\n.\r\n' with '\r\n..\r\n' according to the smtp spec."
  [s]
  (.replaceAll s "\r\n.\r\n" "\r\n..\r\n"))

(defn- write-message-body
  "Takes a writer and a message, writes the body, adding the lone '.' to close the message. returns a string that contains response message."
  [rdr wtr message-body]
  (send-command rdr wtr (str (escape-msg message-body) "\r\n.")))

(defn- quit
  "Takes a writer and sends a 'QUIT' command to the server and returns the 
  response line as a string. Sends the message to the server to disconnect,
  but doesn't close the underlying connections."
  [rdr wtr]
  (send-command rdr wtr "QUIT"))

(defn send-mail
  "Sends an email using the parameters passed, designed to work with gmail.
  The version that takes host and port has not been tested with other smtp relays
  and should be considered experimental at best.
  
  The email addresses should not be surrounded in <>'s and the user and password will be
  base64 encoded according to the spec for you.
  
  Error-handling is non-existent in this version; it's important to use a reasonable socket-timeout
  or use the function without the time-out parameter, which will default to 60 seconds.
  
  Pipelining, which is advertised by gmail, is not used in this implementation. "
  ([from-address to-address subject message-body user passwd host port socket-timeout]
  (with-open [socket (Socket. host port)
              rdr (io/reader socket)
              wtr (io/writer socket)]
    (.setSoTimeout socket socket-timeout)
    (println (.readLine rdr))
    (ehlo rdr wtr host)
    (init-secure-session rdr wtr)
    (with-open [secsocket (.createSocket (SSLSocketFactory/getDefault) socket host port false)
                secreader (io/reader secsocket)
                secwriter (io/writer secsocket)]
      (.startHandshake secsocket)
      (ehlo secreader secwriter host)
      (auth secreader secwriter user passwd)
      (add-from secreader secwriter from-address)
      (if (string? to-address)
        (add-to secreader secwriter to-address)
        (add-all-to secreader secwriter to-address))
      (start-message secreader secwriter)
      (write-message-header secwriter from-address to-address subject)
      (write-message-body secreader secwriter message-body)
      (quit secreader secwriter))))
  
  ([from-address to-address subject message-body passwd socket-timeout]
   (send-mail from-address to-address subject message-body from-address passwd "smtp.gmail.com" 587 socket-timeout))

  ([from-address to-address subject message-body passwd]
   (send-mail from-address to-address subject message-body from-address passwd "smtp.gmail.com" 587 60000)))
