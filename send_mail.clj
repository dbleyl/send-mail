(ns send-mail
  (use clojure.java.io)
  (use base64)
  (import java.net.Socket)
  (import [javax.net.ssl SSLSocketFactory SSLSocket]))

(defn- init-secure-session
  "Send the STARTTLS command to initiate the secure communication session."
  [rdr wtr]
  (.write wtr "STARTTLS\r\n")
  (.flush wtr)
  (.readLine rdr))

(defn- ehlo
  "Invokes EHLO command, and processes response."
  [rdr wtr]
  (.write wtr "EHLO smtp.gmail.com\r\n")
  (.flush wtr)
  (loop [line (.readLine rdr)]
    (println line)
    (if (= \- (.charAt line 3)) ;TODO extract the 'end-of-file' check for multiline responses. SMTP uses a '-' between code and text to indicate another line.
      (recur (.readLine rdr)))))

(defn- auth
  "Sends the user string to the server, returns the response string."
  [rdr wtr user passwd]
  (.write wtr "AUTH LOGIN\r\n")
  (.flush wtr )
  (.readLine rdr)
  (.write wtr (str (encode user) "\r\n"))
  (.flush wtr)
  (.readLine rdr)
  (.write wtr (str (encode passwd) "\r\n"))
  (.flush wtr )
  (.readLine rdr))

(defn- add-from
  "Sends the 'MAIL FROM' command; returns the response string."
  [rdr wtr from-address]
  (.write wtr (str "MAIL FROM:<" from-address ">\r\n"))
  (.flush wtr)
  (.readLine rdr))

(defn- add-to
  "Sends the 'MAIL TO' command; returns the response string."
  [rdr wtr to-address]
  (.write wtr (str "RCPT TO:<" to-address ">\r\n"))
  (.flush wtr)
  (.readLine rdr))

(defn- start-message
  "initiates the message by sending the DATA command; returns a string."
  [rdr wtr]
  (.write wtr "DATA\r\n") 
  (.flush wtr)
  (.readLine rdr))

(defn- write-message-header
  "takes a reader, writer, from-address, to-address and subject, does not
  return a value."
  [wtr from-address to-address subject]
  (.write wtr (str "From: <" from-address ">\r\n"))
  (.write wtr (str "To: <" to-address ">\r\n"))
  (.write wtr
          (str "Date: " 
               (.format 
                 (java.text.SimpleDateFormat. "EEE, d MMM yyyy HH:mm:ss Z") 
                 (java.util.Date.))
               "\r\n"))
  (.write wtr (str "Subject: " subject "\r\n")))

(defn- write-message-body
  "Takes a writer and a message, writes the body, adding the lone '.' to close the message. returns a string that contains response message."
  [rdr wtr message-body]
  (.write wtr message-body) 
  (.write wtr "\r\n.\r\n")
  (.flush wtr)
  (.readLine rdr))

(defn- quit
  "Takes a writer and sends a 'QUIT' command to the server and returns the 
  response line as a string. Sends the message to the server to disconnect,
  but doesn't close the underlying connections."
  [rdr wtr]
  (.write wtr "QUIT\r\n")
  (.flush wtr)
  (.readLine rdr))

(defn send-mail
  "Sends an email using the gmail smtp infrastructure.
  all parameters are required."
  [from-address to-address subject message-body user passwd]
  (with-open [socket (Socket. "smtp.gmail.com" 587)
              rdr (reader socket)
              wtr (writer socket)]
    (.setSoTimeout socket 60000)
    (println (.readLine rdr))
    (ehlo rdr wtr)
    (init-secure-session rdr wtr)
    (with-open [secsocket (.createSocket (SSLSocketFactory/getDefault) socket "smtp.gmail.com" 587 false)
                secreader (reader secsocket)
                secwriter (writer secsocket)]
      (.startHandshake secsocket)
      (ehlo secreader secwriter)
      (auth secreader secwriter user passwd)
      (add-from secreader secwriter from-address)
      (add-to secreader secwriter to-address)
      (start-message secreader secwriter)
      (write-message-header secwriter from-address to-address subject)
      (write-message-body secreader secwriter message-body)
      (quit secreader secwriter))))
