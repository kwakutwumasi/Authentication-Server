# TOTP Login Module

A JAAS login module that connects to the TOTP server and initiates logins. 

To attempt a direct login (i.e, connect to the TOTP device directly to request login) simply pass the username as the password.

The Module has only one required option:

```
totp.url - the URL of the TOTP server (edge or main server)
```