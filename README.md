# AndroCrypt
<img width="700" height="394" alt="android-13" src="https://github.com/user-attachments/assets/56b960b4-389a-4351-9584-a60044378361" />


# AndroCrypt - Android Ransomware 

![Android](https://img.shields.io/badge/Android-13%2B-3DDC84?logo=android&logoColor=white)
![Security](https://img.shields.io/badge/Security-Ransomware-red)
![Educational](https://img.shields.io/badge/Purpose-Educational-blue)

![Easy Encryption](https://github.com/user-attachments/assets/fe3a9288-b503-42c4-bd3b-b9aff65e64ca)


**⚠️ Important Warning: This is strictly for learning and research purposes only! ⚠️**

## What is AndroCrypt?

AndroCrypt is a Android ransomware that shows how modern ransomware operates on newer Android devices (version 13 and up). It fully encrypts mostly every file on the Device and externel (SSD-Cards)


## What Can It Do?

### The Main Features
- **File Encryption**: Locks up your files using strong AES-256 encryption
- **Screen Locking**: Takes over your screen so you can't use your phone
- **Fast Processing**: Encrypts files quickly without slowing down the device
- **Proper Decryption**: Can actually unlock your files with the right key
- **Server Reporting**: Sends infection details to a monitoring server (C&C) or webhook
- **Decryption key system** sends Decryption key from specific infected device ID to a server(C&C) or webhook
- **Decryption key system 2** automatically generates a random decryption key that will get send to your server(C&C)/webhook.
- **Startup** The AndroCrypt software starts on boot so even if the device is shutdown or rebooted it will continue displaying the ransom message screen



### Files It Can Encrypt
Basically, it goes after all the important stuff:
- Documents (PDF, Word, Excel, text files)
- Photos and images (JPG, PNG, etc.)
- Videos and music files
- Zip files and archives
- Even app files and databases

- full supported encryption file extensions here: [File_extensions](https://github.com/Sh1r0ko11/AndroCrypt/blob/main/file_extensions.txt)


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



