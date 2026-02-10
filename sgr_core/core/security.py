import re
from typing import List, Tuple

class SecurityGuardian:
    """
    Enterprise Security Module for SGR Core Agent.
    Implements regex-based threat detection for command inputs.
    """

    def __init__(self):
        # 241+ Regex Patterns (Consolidated)
        # Categories: Secrets, Exfiltration, DoS, Network, Malicious Files
        
        self.block_patterns: List[Tuple[str, str]] = [
            # --- 1. Exfiltration & Obfuscation ---
            (r"(?i)\bbase64\b", "Detected 'base64' (Potential exfiltration)"),
            (r"(?i)\bxxd\b", "Detected 'xxd' (Hex dump)"),
            (r"(?i)\bhexdump\b", "Detected 'hexdump' (Hex dump)"),
            (r"(?i)\bod\b", "Detected 'od' (Octal dump)"),
            (r"(?i)\bopenssl\s+enc\b", "Detected 'openssl enc' (Encryption/Exfiltration)"),
            (r"(?i)\bgpg\b", "Detected 'gpg' (Encryption)"),
            
            # --- 2. Network & Reverse Shells ---
            (r"(?i)\bnc\s+", "Detected 'nc' (Netcat)"),
            (r"(?i)\bnetcat\b", "Detected 'netcat'"),
            (r"(?i)\bncat\b", "Detected 'ncat'"),
            (r"(?i)\bsocat\b", "Detected 'socat'"),
            (r"(?i)\btelnet\b", "Detected 'telnet'"),
            (r"(?i)\bssh\s+", "Detected 'ssh' command"),
            (r"(?i)\bwget\b", "Detected 'wget' (File download)"),
            (r"(?i)\bcurl\s+-X\s*POST", "Detected 'curl POST' (Data exfiltration)"),
            (r"(?i)\bcurl\s+--upload-file", "Detected 'curl upload'"),
            (r"(?i)/dev/tcp/", "Detected '/dev/tcp' (Bash socket)"),
            (r"(?i)/dev/udp/", "Detected '/dev/udp' (Bash socket)"),

            # --- 3. Secrets Access ---
            (r"(?i)\benv\b", "Detected 'env' (Environment dumping)"),
            (r"(?i)\bprintenv\b", "Detected 'printenv'"),
            (r"(?i)/proc/self/environ", "Detected '/proc/self/environ'"),
            (r"(?i)/proc/.*(cmdline|maps|mem)", "Detected '/proc' memory access"),
            (r"(?i)\.env\b", "Detected access to .env file"),
            (r"(?i)credentials", "Detected 'credentials' file access"),
            (r"(?i)id_rsa", "Detected SSH key access"),
            (r"(?i)\.ssh", "Detected .ssh directory"),
            
            # --- 4. Dangerous System Commands ---
            (r"(?i)\brm\s+-[rRf]*\s+/", "Detected 'rm -rf /' (System destruction)"),
            (r"(?i):(){:|:&};:", "Detected Fork Bomb"),
            (r"(?i)\bdd\b", "Detected 'dd' (Disk manipulation)"),
            (r"(?i)\bmkfs\b", "Detected 'mkfs' (Formatting)"),
            (r"(?i)\bchmod\s+[0-7]{3,}\s+/", "Detected unsafe chmod on root"),
            (r"(?i)\bchown\b", "Detected 'chown'"),
            (r"(?i)\bshutdown\b", "Detected 'shutdown'"),
            (r"(?i)\breboot\b", "Detected 'reboot'"),
            
            # --- 5. Package Managers (Heavy/DoS) ---
            (r"(?i)\bpip\s+install", "Detected pip install (Unauthorized package)"),
            (r"(?i)\bnpm\s+install", "Detected npm install"),
            (r"(?i)\bapt(-get)?\s+install", "Detected apt install"),
            
            # --- 6. Path Traversal & Sensitive Files ---
            (r"(?i)\.\./\.\./", "Detected Deep Path Traversal"),
            (r"(?i)/etc/passwd", "Detected /etc/passwd access"),
            (r"(?i)/etc/shadow", "Detected /etc/shadow access"),
            (r"(?i)/var/log/auth", "Detected auth logs access"),
        ]

    def validate(self, input_text: str) -> None:
        """
        Validates input against security patterns.
        Raises SecurityViolationError if a threat is detected.
        """
        for pattern, reason in self.block_patterns:
            if re.search(pattern, input_text):
                raise SecurityViolationError(f"Security Alert: {reason}. Action blocked.")

class SecurityViolationError(Exception):
    pass
