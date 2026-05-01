# 🧠 LocalMind: Offline AI Tutor

> **𝐁𝐮𝐢𝐥𝐝𝐢𝐧𝐠 𝐚𝐧 𝐀𝐈 𝐭𝐡𝐚𝐭 𝐰𝐨𝐫𝐤𝐬 𝐰𝐡𝐞𝐫𝐞 𝐭𝐡𝐞 𝐢𝐧𝐭𝐞𝐫𝐧𝐞𝐭 𝐝𝐨𝐞𝐬𝐧’𝐭.**

LocalMind is an Android-based EdTech application built during a 24-hour hackathon. It brings the power of Generative AI directly to the student's device. By combining a highly optimized, locally running Large Language Model with Retrieval-Augmented Generation (RAG), LocalMind acts as an interactive tutor grounded strictly in textbook data.

## ✨ Key Features

*   **Interactive Offline Chatbot:** A dedicated, easy to use chat interface powered by a locally running quantized LLM. Delivers instant, offline tutoring with absolute data privacy and zero network latency.
*   **Dashboard & Progress Tracking:** A central user hub that visualizes student learning milestones and tracks quiz performance over time to keep users motivated and on target.
*   **Dynamic Quiz Generation:** Uses the on-device AI to automatically generate interactive multiple choice questions based on the study material to test knowledge retention.
*   **Multilingual & Voice-Ready:** Seamlessly supports native Android voice-typing and automatically detects user intent to reply naturally in English, Hindi (Devanagari), or Hinglish.
*   **Strict RAG Grounding (WIP):** Employs a local FAISS vector database to anchor the AI's knowledge strictly to official curriculum data, effectively reducing hallucinations.

## 🏗️ System Architecture

Our solution bridges a Python-based data processing pipeline with an efficient C++ execution engine on Android.

1.  **The LLM Engine:** Utilizes a quantized **Llama-3.2-1B-Instruct** model loaded directly into the Android device's internal storage.
2.  **The JNI Bridge (C++):** A custom Java Native Interface connects the Kotlin Android UI directly to the `llama.cpp` engine for high-performance mobile inference.
3.  **The RAG Pipeline (Python):** Textbooks are chunked and embedded using `sentence-transformers`. The resulting FAISS index is packaged into the app. *(Currently optimizing context window handling for mobile RAM!)*

## 🛠️ Tech Stack

*   **Frontend:** Android (Kotlin, XML Layouts)
*   **Core Engine:** C++, `llama.cpp`, JNI
*   **Data Processing:** Python, FAISS, JSON
*   **Machine Learning:** HuggingFace SentenceTransformers, Meta Llama 3.2

## 🚀 Installation & Setup

### Prerequisites
*   Android Studio
*   A physical Android device or Emulator with at least 4GB of allocated RAM.

### 1. Download the Model
*Note: Due to GitHub file size limits, the LLM weights are not included in this repository.*
1. Download the `Llama-3.2-1B-Instruct-Q8_0.gguf` from HuggingFace.
2. Open Android Studio and use the **Device File Explorer**.
3. Push the `.gguf` file to the device's internal storage exactly at this path: `/data/user/0/com.example.localmind/files/models/`

### 2. Build and Run
1. Clone this repository to your local machine.
2. Open the project root in Android Studio.
3. Allow Gradle to sync and the NDK/CMake to compile the C++ bridge.
4. Build the APK and deploy it to your device.

## 🔮 Future Scope (V2)

*   Refining the C++ bridge to perfectly handle context windows alongside JNI UTF-8 encoding for Devanagari script during RAG retrieval.
*   Expanding curriculum support for Grades 7-12 across multiple subjects.
*   Adding image recognition to solve offline math problems step-by-step.

## 👨‍💻 Meet the Team

*   **Alok Sahu** - *Focused on LLM integration on android studio, JNI architecture and C++ Bridge.*
*   **Yashraj Naapara** - *Focused on Model Procurement, llama.cpp integration and LLM selection.*
*   **Piyush Lilhare** - *Focused on Data Processing, RAG integration, FAISS, JSON and Vector Embedding.*
*   **Lakshya Patole** - *Focused on Android UI/UX and Kotlin architecture.*


## Some Screenshots
<img width="360" height="800" alt="WhatsApp Image 2026-05-02 at 00 00 06" src="https://github.com/user-attachments/assets/661c0e4a-9cf4-4166-810c-9a08fc9f3094" />
<img width="360" height="800" alt="WhatsApp Image 2026-05-02 at 00 00 06 (1)" src="https://github.com/user-attachments/assets/b15bec3f-1b77-45a4-bac8-a01a348f6f70" />
<img width="360" height="800" alt="WhatsApp Image 2026-05-02 at 00 01 56" src="https://github.com/user-attachments/assets/11d31cb0-7653-4656-95fe-edd891c0f7f1" />
<img width="360" height="800" alt="WhatsApp Image 2026-05-02 at 00 01 57" src="https://github.com/user-attachments/assets/2dae7326-448b-476c-86c3-899717d72d06" />
<img width="360" height="800" alt="WhatsApp Image 2026-05-02 at 00 01 57 (1)" src="https://github.com/user-attachments/assets/18a1b304-2900-41eb-94a5-0bf8b8ee7a94" />

---
*Built during TechnoTarang Hackathon*
