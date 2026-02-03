import os
import uuid
import warnings
import google.generativeai as genai
from typing import Dict, List
import json
import random

warnings.filterwarnings("ignore")


class ChatbotService:
    def __init__(self, api_key: str = None):
        """Initialiser le service chatbot avec l'API Gemini"""
        # --- Récupérer la clé API depuis les variables d'environnement si non fournie ---
        if api_key is None:
            api_key = os.getenv("GEMINI_API_KEY")

        if not api_key:
            raise ValueError(
                "GEMINI_API_KEY non configurée ! "
                "Définissez-la dans votre terminal ou fichier .env"
            )

        # Configurer Gemini
        genai.configure(api_key=api_key)
        self.model = genai.GenerativeModel(model_name="gemini-2.5-flash")

        # Conversations utilisateur
        self.sessions: Dict[str, List[dict]] = {}

        # Évaluations pour le mode SCORE
        self.evaluations: Dict[str, List[dict]] = {}

        # Prompt système de base
        self.system_prompt = """
Tu es DebateMaster, un expert en argumentation et en débats.
Tu as deux modes :

--------------------------------------------------------
MODE 1 = "train"
Objectif : entraîner l'utilisateur à débattre.
- Réponds comme un expert du débat
- Propose des arguments logiques
- Contredis ou soutiens selon la discussion
- Donne des conseils si l'utilisateur fait une erreur
- Ne donne JAMAIS de score dans ce mode

--------------------------------------------------------
MODE 2 = "score"
Objectif : évaluer la qualité argumentative de l'utilisateur.
À chaque message utilisateur :
- Analyse l'idée principale
- Analyse la cohérence logique
- Analyse l'utilisation de preuves
- Analyse la force argumentative
- Analyse la clarté du style
- Génère une mini-évaluation (score 0–20 pour chaque critère)
Stocke tout cela mais NE RÉVÈLE PAS encore le score.

Quand l'utilisateur dit "fin du débat" :
- Fournis un rapport complet :
  * Score global /100
  * Forces
  * Faiblesses
  * Conseils d'amélioration
"""

    def generate_response(self, message: str, mode: str = "train", session_id: str = None) -> dict:
        """Générer une réponse du chatbot avec support des deux modes."""
        # --- Créer ou récupérer la session ---
        if not session_id:
            session_id = str(uuid.uuid4())

        if session_id not in self.sessions:
            self.sessions[session_id] = []
            self.evaluations[session_id] = []

        # --- Enregistrer le message utilisateur ---
        self.sessions[session_id].append({"role": "user", "content": message})

        # --- Mode SCORE : analyser les arguments utilisateur ---
        if mode == "score" and message.lower() not in ["fin du débat", "fin", "score"]:
            analysis = self._evaluate_argument(message)
            self.evaluations[session_id].append(analysis)

        # --- Score final ---
        if mode == "score" and message.lower() in ["fin du débat", "fin", "score"]:
            final_report = self._generate_final_score(session_id)
            self.sessions[session_id].append({"role": "assistant", "content": final_report})
            return {"text": final_report, "session_id": session_id}

        # --- Générer la réponse du chatbot ---
        try:
            context = self._build_context(session_id)
            full_prompt = f"{self.system_prompt}\n\nMODE ACTUEL : {mode}\n\n{context}\nUtilisateur : {message}"

            response = self.model.generate_content(full_prompt)
            response_text = response.text

            # Sauvegarder réponse IA
            self.sessions[session_id].append({"role": "assistant", "content": response_text})

            return {"text": response_text, "session_id": session_id}

        except Exception as e:
            raise Exception(f"Erreur génération IA : {str(e)}")

    def _build_context(self, session_id: str) -> str:
        """Reconstruire le contexte des derniers échanges."""
        history = self.sessions.get(session_id, [])[-10:]
        context = ""
        for msg in history:
            role = "User" if msg["role"] == "user" else "Assistant"
            context += f"{role}: {msg['content']}\n"
        return context

    def _evaluate_argument(self, message: str) -> dict:
        """Analyse automatique d'un argument utilisateur (mode score)."""
        prompt = f"""
Analyse ce message d'utilisateur pour un débat :

Message : "{message}"

Donne une analyse sous forme de JSON avec :
- idee_principale (texte)
- logique (score 0-20)
- preuves (score 0-20)
- force_argumentative (score 0-20)
- structure (score 0-20)
- clarte_style (score 0-20)
"""
        try:
            response = self.model.generate_content(prompt)
            result = json.loads(response.text)

            for key in ["logique", "preuves", "force_argumentative", "structure", "clarte_style"]:
                if key not in result or not isinstance(result[key], (int, float)):
                    result[key] = 15

            if "idee_principale" not in result:
                result["idee_principale"] = message[:50]

            return result

        except Exception:
            return {
                "idee_principale": message[:50],
                "logique": 15,
                "preuves": 15,
                "force_argumentative": 16,
                "structure": 15,
                "clarte_style": 16
            }

    def _generate_final_score(self, session_id: str) -> str:
        """
        Génère TOUJOURS un score final simulé entre 70 et 100 (mode démo).
        """
        score_final = random.randint(70, 100)

        rapport = f"""
🎯 *Score final du débat : {score_final}/100*

✅ *Points forts*
- Bonne compréhension du sujet
- Argumentation globalement cohérente
- Effort de structuration des idées

❌ *Points à améliorer*
- Approfondir certains arguments
- Ajouter davantage d'exemples
- Clarifier certaines formulations

📘 *Conseils*
- Structurer chaque argument (idée → justification → exemple)
- Anticiper les contre-arguments
- Appuyer les affirmations par des faits
"""
        return rapport

    def clear_session(self, session_id: str):
        """Efface l'historique complet d'une session"""
        if session_id in self.sessions:
            del self.sessions[session_id]
        if session_id in self.evaluations:
            del self.evaluations[session_id]
