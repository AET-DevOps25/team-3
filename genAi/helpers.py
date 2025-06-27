import base64
import os


def save_document(document_name: str, document_base64: str) -> str:
    """
    Save the document to a file.
    Use document_name to create a unique file name.
    return the file path of the saved document.
    """
    # Create a directory for the documents if it doesn't exist
    documents_dir = "./documents"
    os.makedirs(documents_dir, exist_ok=True)

    # Decode the base64 document
    document_bytes = base64.b64decode(document_base64)
    
    # Create a unique file name
    file_path = os.path.join(documents_dir, document_name)

    # Save the document to a file
    with open(file_path, "wb") as file:
        file.write(document_bytes)
    
    print("Current working dir:", os.getcwd())
    print(f"Document {document_name} saved successfully at {file_path}")
    return file_path

def delete_document(document_path: str):
    """
    Delete the document file.
    """
    if '/example/' in document_path:
        # Skip deletion for example documents
        return
    
    if os.path.exists(document_path):
        os.remove(document_path)
        print(f"Document {document_path} deleted successfully.")
    else:
        print(f"Document {document_path} does not exist.")