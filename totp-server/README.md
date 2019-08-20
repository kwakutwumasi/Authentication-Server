# TOTP-Server
A Server implementing time based one time PINs (TOTP's) as described in [RFC 6238](https://tools.ietf.org/html/rfc6238). It can be integrated into Authentication Server with some minor adjustments, or deployed as a standalone service using QA-Appbase or any fully certified JEE 7+ server.

### Operation

The server has a REST interface for management and provisioning. The management interface allows administrators to 
* create device aliases,
* lock and unlock devices, 
* create additional administrator devices, and
* deactivate devices

### Devices

A device is defined as any software or hardware capable of generating tokens using the [RFC 6238](https://tools.ietf.org/html/rfc6238) algorithm. The HMAC algorithm and expected length must be set to the same values, or else tokens will not match. An example Android Implementation is provided in a separate project.

Devices must be provisioned before usage. Provisioning involves the generation of a cryptographic seed and the initial counter. These are transmitted to the device for secure storage over a secure communication channel.

Once provisioned, a device must be activated by sending a generated token to the server. The server validates the token and sets the device to active. The device can then be authenticated by the server with a new time based token.

The devices provide their own ID's during provisioning. ID's are checked for uniqueness, and as such it is important to use unique identifiers. An example of a unique identifier would be the Android Device ID or the IMIE number (for smartphones). Device creators should take this into account when programming their device for provisioning. The sample application uses the Android Device ID as the unique identifier.

### Aliases

Aliases can be used to associate an application specific identifier to a device. 
As an example a bank may want its customers to log on to an Internet Banking platform using a bank account numbers. If the device ID in use is not the same as this account number, clients would have to enter the device id during login. The account number can be setup on the TOTP server as an alias for the customer specific device. During authentication, the account number can be used in place of the device id. The server will look for the device record using the alias.

Multiple aliases can be assigned to a single device. 

Aliases must be unique.

### Encryption

The server uses encryption to store seeds and other security values in a database. See QA-Crypto for more information on how to setup encryption.

For the highest level of security, a Hardware Security Module (HSM) should be used. Any HSM that provides a Java Cryptography Extension (JCE) module can be used with QA-Crypto. A HSM will protect the encryption keys from being pulled off the server, ensuring that only authorized applications on the server can encrypt and decrypt secured information.

### Security Features

Aside from encryption the server has some security features to prevent tampering.

To protect against device ID switching, an encrypted check value is stored in the database. This check value is verified each time the device is retrieved. If an attacker manages to gain access to the database and tries to switch their device ID with that of a target, they would need to generate and encrypt the check value. If the encryption key is stored securely, this will not be feasible.

The same protection exists for aliases and administrator accounts. 

### Edge Servers

This server is not meant to exposed to external untrusted networks (such as the internet). It is intended to be firewalled and accessed by edge servers, that in turn may be protected by web application firewalls for maximum security.
The edge server has been implemented in this repository.

### Direct Authentication

Edge servers may connect to this server. The "device.connection" parameters of the configuration file provide details for the server port that edge servers may connect to. Once connected, the server may attempt to authenticate users by sending a request to the device directly. The details of edge server communication with the authentication device is covered by the edge server documentation.

### Setup and Configuration

###### Using Authentication Server

The libraries for the TOTP server must be included in the Authentication Server class path. Since Authentication Server uses the QA-Appbase, the basic platform for running the login module is in place. Authentication Server uses JAX-RS to provide its interface. The web server within Authentication Server will automatically load the REST interfaces for TOTP. 

The server will require an implementation of QA-ORM, specifically a Java Persistent Architecture compatible implementation. Non JPA implementations will work. However they must support transparent encryption and decryption of the secured fields, using QA-Crypto's _com.quakearts.security.cryptography.jpa.EncryptedValueConverter_ and _com.quakearts.security.cryptography.jpa.EncryptedValueStringConverter_. 

The reference implementation uses QA-ORM-Hibernate as its ORM implementation. QA-ORM-Hibernate requires a Java Connectivity Architecure (JCA) datasource. Since Authentication server excludes these libraries, they would need to be explicitly added. The Java Transacton Architecture (JTA) libraries can also be added to simplify transaction management.

Two other files need to be on the classpath:
1. login.config - this is a JAAS login configuration for authenticating tokens used to access the management interface. It must be configured with an appropriate _com.quakearts.webapp.security.auth.JWTLoginModule_ named 'TOTP-JWT-Login'. See QA-Auth for more information on configuring _JWTLoginModule_.

2. totpoptions.json - This file contains important setup information for the TOTP server:

```
data.store.name - The name of the QA-ORM datastore for the TOTP server
mac.algorithm - The specific HMAC algorithm to use to generate the tokens 
mac.provider - The JCE provider of the HMAC generation algorithm
otp.length - the OTP string length
seed.length - the length of the seed to generate, in bytes. It must be appropriate for the selected MMAC algorithm
secure.random.instance - The secure random instance to use when generating the seed
secure.random.provider - the JCE provider of the secure random instance
time.step - the amount of time in seconds that the token is valid for
grace.period - the amount time in seconds to consider tokens generated in the previous time step. This is to allow for network latency and un-syncronized clocks
max.attempts - the maximum number of tries before a device is locked
lockout.time - the amount of time, in microseconds before the lockout tries are reset. If a device has been locked, the reset will have no effect.
installed.administrators - the initial list of device administrators. These must be setup prior to server initiation and prior to provisioning of the devices.
count.query - The SQL server query to use to when pulling the total device count (Only necessary for SQL server based QA-ORM implementations)
device.connection -> port - The port edge servers connect to
					  ->keystore - the JCE Keystore for TLS encryption
					  ->keystore.type - the JCE Keystore type for TLS encryption
					  ->keystore.password - the JCE Keystore password (if any) for TLS encryption
					  ->keystore.provider - the JCE Keystore provider for TLS encryption
					  ->socket.timeout - The timeout period for socket connections to the edge servers
					  ->ssl.instance - the SSL/TLS version to use
```

###### Stand Alone

The TOTP server is already integrated into QA-Appbase and once the necessary libraries are on the classpath, the server can be started. Additional files are required. These can be copied from the src/test/resources folder of this project and amended as required.

###### Setup Notes

Prior to the first server initialization, a minimum of two (2) devices must be selected as administrators. One device is used to create records. The other is used to approve the record creation. 

The selected devices will need to generate device ID's prior to provisioning. These device ID's are configured in the applications setup file _totpoptions.json_ in the 'installed.administrators' section of the configuration. Below is an example of the entries: 

```
	"installed.administrators":{
		"map":{"testadministrator1":"Adminstrator 1","testadministrator2":"Adminstrator 2"}
	}
```

The map key is the device ID and the value is the name given to that device. It should be descriptive such as "Kofi Babone's iPhone 6S" or "John Smith's Samsung Galaxy 5" to make it easy for administrators to know the owner and the specific device.

Once this and other parameters have been set, the server can be started and the administrator device provisioned. The devices will be added to the set of administrator devices during provisioning.

It is important to follow these steps in order. Failing to do so, the TOTP server will not have any administrator devices, and any modification to the system through the Management interface will be impossible.

### The Authentication Algorithm

See [RFC 6238](https://tools.ietf.org/html/rfc6238) for more details on how the algorithm works. The TOTP server implementation is as follows:

1) Calculate time count T by subtracting current time Tn from initial counter/time T0, then integer divide by the time step value TS. For languages that can't do integer division natively (like Javascript) use a floor function to round the remainder down to the first integer less than the remainder.

2) Convert the integer to an eight byte value, and append the character bytes of the device ID

3) Use the selected HMAC algorithm and the secret seed value to generate the HMAC of the value from step 2

4) Calculate an offset value using the absolute value of the last byte of the HMAC result modulo the length of the HMAC result minus four (4) i.e.

```
	offset = absolute(hmacresult[0]) mod (hmacresult.length - 4)
```

5) Combine the four byte values starting from the offset into an integer. An example from the TOTP server code (Java):

```java
int code = (hmacresult[offset] & 0x7f) << 24 |
				(hmacresult[offset+1] & 0xff) << 16 |
				(hmacresult[offset+2] & 0xff) << 8 |
				hmacresult[offset+3] & 0xff;
```

6) Calculate the OTP token as the resulting integer mod 10 raised to the power of the selected token length i.e.

```
int tokencode = code mod 10^(otp length)
```

The last step will shorten the (possibly) longer integer to at most the length required.

A reference implementation of an authentication device is provided in this repository.