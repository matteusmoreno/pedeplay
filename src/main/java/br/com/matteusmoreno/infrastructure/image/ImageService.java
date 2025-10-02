package br.com.matteusmoreno.infrastructure.image;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.util.Map;

@ApplicationScoped
public class ImageService {

    private final Cloudinary cloudinary;

    public ImageService(
            @ConfigProperty(name = "cloudinary.cloud-name") String cloudName,
            @ConfigProperty(name = "cloudinary.api-key") String apiKey,
            @ConfigProperty(name = "cloudinary.api-secret") String apiSecret) {

        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }

    public String uploadImage(byte[] imageBytes, String publicId) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(imageBytes, ObjectUtils.asMap(
                "public_id", publicId,   // Define um nome/ID customizado para a imagem
                "overwrite", true,      // Se já existir uma imagem com esse ID, sobrescreve
                "resource_type", "image"
        ));

        return (String) uploadResult.get("secure_url");
    }

    public void deleteImage(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}