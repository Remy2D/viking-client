from .PBotInventory import PBotInventory
from typing import Optional, List, Tuple


class PBotWindow(object):
    def __init__(self, window):
        self.__window = window

    ## Tooltips of widgets in the window, example: Trough
    # @return list containing the tooltips that were found
    def get_tooltips(self) -> List[str]:
        return list(self.__window.getTooltips())

    ## Get amounts of meters of the window, from 0 to 100, some windows may have more than 1 meter, like chicken coops
    # @return list containing amounts of the meters that were found
    def get_amounts(self) -> List[int]:
        return list(self.__window.getAmounts())

    ## Returns used capacity of the stockpile window which is currently open
    # @return used capacity, or -1 if stockpile window could not be found
    def get_stockpile_used_capacity(self) -> int:
        return self.__window.getStockpileUsedCapacity()

    ## Put an item from the hand to a stockpile window that is currently open
    def put_item_from_hand_to_stockpile(self):
        self.__window.putItemFromHandToStockpile()

    ## Attempts to get items from the stockpile that is currently open
    # @param count how many items to take
    def take_items_from_stockpile(self, count: int = 1):
        self.__window.takeItemsFromStockpile(count)

    ## Attempts to put item that fits form inventory to the stockpile, like when scrolling to stockpile
    # @param count how many items to put in the stockpile
    def put_item_from_inventory_to_stockpile(self, count: int = 1):
        self.__window.putItemFromInventoryToStockpile(count)

    ## Total capacity of the stockpile, if the window is stockpile
    # @return total capacity or -1 if stockpile couldn't be found
    def get_stockpile_total_capacity(self) -> int:
        return self.__window.getStockpileTotalCapacity()

    ## Tries to find an inventories attached to the given window, such as cupboard
    # @return list of inventories attached
    def get_inventories(self) -> List[PBotInventory]:
        return [PBotInventory(x) for x in self.__window.getInventories()]

    ## Close this window
    def close(self):
        self.__window.closeWnd()
