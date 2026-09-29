package com.messmate.android.ui.manager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.messmate.android.domain.model.MemberInfo
import com.messmate.android.domain.model.UserRole
import com.messmate.android.ui.common.EmptyState
import com.messmate.android.ui.common.StatusBadge
import com.messmate.android.ui.theme.SageGreen
import com.messmate.android.ui.theme.Terracotta

@Composable
fun ManagerMembersScreen(
    state: ManagerUiState,
    onSearchQueryChange: (String) -> Unit,
    onSortOptionChange: (MemberSortOption) -> Unit,
    onUpdateRoom: (membershipId: String, newRoom: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingMember by remember { mutableStateOf<MemberInfo?>(null) }

    // Filter & Sort
    val displayedMembers = remember(state.members, state.memberSearchQuery, state.memberSortOption) {
        val query = state.memberSearchQuery.trim()
        val filtered = if (query.isBlank()) {
            state.members
        } else {
            state.members.filter {
                it.fullName.contains(query, ignoreCase = true) ||
                it.roomNumber?.contains(query, ignoreCase = true) == true ||
                it.email.contains(query, ignoreCase = true)
            }
        }

        when (state.memberSortOption) {
            MemberSortOption.NAME -> filtered.sortedBy { it.fullName.lowercase() }
            MemberSortOption.ROOM -> filtered.sortedWith(
                compareBy(nullsLast()) { it.roomNumber }
            )
            MemberSortOption.BALANCE -> filtered.sortedBy { it.netBalance }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = state.memberSearchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search by name, room, or email...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (state.memberSearchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Sort Options Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${displayedMembers.size} Member(s)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(MemberSortOption.values()) { option ->
                    val isSelected = state.memberSortOption == option
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSortOptionChange(option) },
                        label = {
                            Text(
                                when (option) {
                                    MemberSortOption.NAME -> "Name (A-Z)"
                                    MemberSortOption.ROOM -> "Room"
                                    MemberSortOption.BALANCE -> "Balance"
                                }
                            )
                        }
                    )
                }
            }
        }

        // Member list
        if (displayedMembers.isEmpty()) {
            EmptyState(
                title = "No members found",
                description = "No mess members matched your search query.",
                icon = Icons.Rounded.PersonSearch
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(displayedMembers, key = { it.id }) { member ->
                    val isUpdatingRoom = state.updatingRoomMembershipIds.contains(member.id)
                    MemberCard(
                        member = member,
                        isUpdating = isUpdatingRoom,
                        onEditRoom = { editingMember = member }
                    )
                }
            }
        }
    }

    // Edit Room Dialog
    if (editingMember != null) {
        val member = editingMember!!
        var roomText by remember { mutableStateOf(member.roomNumber ?: "") }

        AlertDialog(
            onDismissRequest = { editingMember = null },
            icon = { Icon(Icons.Rounded.MeetingRoom, contentDescription = null, tint = Terracotta) },
            title = { Text("Update Room Number") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Update assigned room for ${member.displayNameWithRoom}:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = roomText,
                        onValueChange = { roomText = it },
                        label = { Text("Room Number (e.g. 302B)") },
                        placeholder = { Text("Room number or label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateRoom(member.id, roomText.trim().takeIf { it.isNotBlank() })
                        editingMember = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
                ) {
                    Text("Save Room")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMember = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MemberCard(
    member: MemberInfo,
    isUpdating: Boolean,
    onEditRoom: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Name — Room X format
                Text(
                    text = member.displayNameWithRoom,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(
                        text = when (member.role) {
                            UserRole.OWNER -> "Owner"
                            UserRole.PRIMARY_MANAGER -> "Primary Manager"
                            UserRole.MANAGER -> "Manager"
                            UserRole.MEMBER -> "Member"
                            UserRole.CHEF -> "Chef"
                            UserRole.STUDENT -> "Student"
                        },
                        color = when (member.role) {
                            UserRole.OWNER -> Terracotta
                            UserRole.PRIMARY_MANAGER, UserRole.MANAGER -> Color(0xFF8B5CF6)
                            UserRole.MEMBER, UserRole.STUDENT -> MaterialTheme.colorScheme.outline
                            UserRole.CHEF -> SageGreen
                        }
                    )

                    member.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                        Text(
                            text = "• $phone",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Net Balance & Edit Room Action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    val bal = member.netBalance
                    val isPositive = bal >= 0
                    Text(
                        text = if (isPositive) "+৳${"%,.2f".format(bal)}" else "-৳${"%,.2f".format(-bal)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) SageGreen else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = if (isPositive) "Surplus" else "Due",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onEditRoom,
                    enabled = !isUpdating,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit Room",
                            tint = Terracotta,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
