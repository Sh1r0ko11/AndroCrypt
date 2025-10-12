# AndroCrypt
<img width="1200" height="713" alt="android-14-376844" src="https://github.com/user-attachments/assets/8966cae7-095c-494d-a54d-098d45660859" />




# AndroCrypt - Android Ransomware 

![Android](https://img.shields.io/badge/Android-14%2B-3DDC84?logo=android&logoColor=white)
![Security](https://img.shields.io/badge/Security-Ransomware-red)
![Educational](https://img.shields.io/badge/Purpose-Educational-blue)

![Easy Encryption](https://github.com/user-attachments/assets/fe3a9288-b503-42c4-bd3b-b9aff65e64ca)


**⚠️ Important Warning: This is strictly for learning and research purposes only! ⚠️**

## What is AndroCrypt?

AndroCrypt is a Android ransomware that shows how modern ransomware operates on newer Android devices (version 14 and up). It fully encrypts mostly every file on the Device and externel (SD-Cards)


## What Can It Do?

### The Main Features
- **File Encryption**: Locks up your files using strong AES-256 encryption
- **Screen Locking**: Takes over your screen so you can't use your phone
- **Fast Processing**: Encrypts files quickly without slowing down the device
- **Proper Decryption**: Can actually unlock your files with the right key
- **Server Reporting**: Sends infection details to a monitoring server (C&C) or webhook (TCP OR HTPPS)
- **Decryption key system** sends Decryption key from specific infected device ID to a server(C&C) or webhook
- **Decryption key system 2** automatically generates a random decryption key that will get send to your server(C&C)/webhook.
- **Startup** The AndroCrypt software starts on boot so even if the device is shutdown or rebooted it will continue displaying the ransom message screen
- **Anti memory Dumping** decryption key SHOULDN'T be possible to dumb out of memory or kernel itself
- **Security** Security features for attacks such as memory dumping and others, were trying to keep our APP safe against malware researchers and people that want to decrypt without having to pay

### Files It Can Encrypt
Basically, it goes after all the important stuff:
- Documents (PDF, Word, Excel, text files)
- Photos and images (JPG, PNG, etc.)
- Videos and music files
- Zip files and archives
- Even app files and databases

- full supported encryption file extensions here: [File_extensions](https://github.com/Sh1r0ko11/AndroCrypt/blob/main/file_extensions.txt)


### Stealing? what does this mean?
AndroCrypt is one of the first Android Ransomwares that Steals passwords from phones, Browser Data and Basic Information.
We designed it to be powerful and easy. Our app isnt just ,,any,, App, its one of the most powerful free **Open-Source** available. its steals like no other. 
# We Make Things Easy 🌟

**Smooth Experience, Instant Trust**

We design our app to feel like familiar, trusted software from the very first use. The experience is so natural and intuitive that users feel immediately comfortable, while everything works seamlessly in the background.

## A Step-by-Step Look

### 1: The Permission Trap
First the app sweet-talks the user into giving it the keys to the kingdom. It presents itself as a helpful tool that needs special access to "optimize" the device.

<div align="center">
<img width="300" alt="The app requesting permissions" src="https://github.com/user-attachments/assets/6a661c9c-69b4-4d52-9092-d6e85901d190" />
</div>

---

### 2: The Fake Cleanup
Once it has access the app puts on a convincing show. It displays a progress bar that makes it look like its hard at work cleaning up junk files and optimizing performance. This is all theater designed to make the user feel like everything is working perfectly.

<div align="center">
<img width="300" alt="The fake cleanup screen" src="https://github.com/user-attachments/assets/6043bd39-264a-4a36-a434-879a6c19a6ac" />
</div>

---

### 3: The Reveal
The curtain drops. After the fake cleanup "finishes" the user is greeted with the ransom note. Their files are encrypted, screen is locked and the app makes it clear that paying up is the only way out.
<div align="center">
<img width="300" alt="The ransom demand screen" src="https://github.com/user-attachments/assets/4337d6b1-4993-4e3c-a211-44d905b5d8cb" />
</div>

### Device Layout:
<img width="488" height="763" alt="Screenshot 2025-09-27 215228" src="https://github.com/user-attachments/assets/f7a2fb5c-fc5a-4980-bb25-bcc69c775ae7" />


## Tools and requirements you need


- **Kotlin**
- **python 3.11**
- **Java**
- **AndroidStudio**
- **VSC** (recommended but no must)
