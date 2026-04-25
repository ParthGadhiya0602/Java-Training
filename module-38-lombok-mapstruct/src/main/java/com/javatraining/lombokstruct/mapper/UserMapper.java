package com.javatraining.lombokstruct.mapper;

import com.javatraining.lombokstruct.dto.CreateUserRequest;
import com.javatraining.lombokstruct.dto.UserDto;
import com.javatraining.lombokstruct.entity.User;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for User ↔ DTO conversions.
 *
 * <p>MapStruct generates a concrete implementation class at compile time.
 * With {@code componentModel = "spring"}, the generated class is annotated
 * with {@code @Component} and registered in the Spring context.
 *
 * <p>The generated source lives at:
 * {@code target/generated-sources/annotations/.../UserMapperImpl.java}
 * Inspect it to understand exactly what MapStruct generates.
 *
 * <p>Key mapping techniques shown here:
 * <ol>
 *   <li>Field ignored: {@code @Mapping(target = "id", ignore = true)}</li>
 *   <li>Expression: combine two fields into one string</li>
 *   <li>Nested source: flatten {@code address.city} → {@code city}</li>
 *   <li>Collection: {@code List<User>} → {@code List<UserDto>} auto-generated</li>
 *   <li>Partial update: null values in source are ignored (IGNORE strategy)</li>
 * </ol>
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Maps a creation request to a User entity.
     *
     * <p>{@code id} is ignored - it will be assigned by the persistence layer.
     * {@code address} is ignored - not present in the creation request.
     */
    @Mapping(target = "id",      ignore = true)
    @Mapping(target = "address", ignore = true)
    User requestToUser(CreateUserRequest request);

    /**
     * Maps a User entity to its response DTO.
     *
     * <p>Two non-trivial mappings:
     * <ul>
     *   <li>{@code fullName} uses a Java expression to combine first and last name.
     *       The expression must compile; MapStruct embeds it verbatim.</li>
     *   <li>{@code city} uses a dotted source path to flatten the nested
     *       {@code address.city} field. MapStruct generates null-safe code:
     *       if {@code address} is null, {@code city} is set to null.</li>
     * </ul>
     */
    @Mapping(target = "fullName",
             expression = "java(user.getFirstName() + \" \" + user.getLastName())")
    @Mapping(target = "city", source = "address.city")
    UserDto userToDto(User user);

    /**
     * Maps a list of users to a list of DTOs.
     *
     * <p>MapStruct auto-generates this from the single-item {@link #userToDto} method.
     * No body needed - the framework infers the element mapping automatically.
     */
    List<UserDto> usersToDtos(List<User> users);

    /**
     * Partial update - applies non-null fields from {@code request} onto {@code user}.
     *
     * <p>{@code NullValuePropertyMappingStrategy.IGNORE} means: if a field in
     * {@code request} is null, the corresponding field on {@code user} is
     * left unchanged.  This enables PATCH semantics without reading and re-writing
     * every field explicitly.
     *
     * <p>{@code @MappingTarget} marks the parameter that MapStruct writes into
     * rather than creating a new instance.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",      ignore = true)
    @Mapping(target = "address", ignore = true)
    void updateFromRequest(CreateUserRequest request, @MappingTarget User user);
}
