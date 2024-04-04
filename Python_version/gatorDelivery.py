from avlTree import treeNode, avlTree

myTree = avlTree()

nodes = {}                  # {orderId: node}
orders = {
    "eta": [],              # ascending order
    "priority": [],         # descending order
    "ID": [],               # id
    "deliveryTime": [],     # delivery time
    "deliveryStatus": [],   # 0: not started, 1: out for delivery, 2: delivered
}
lastReturnTime = 0
currentOrderSize = 0

def getPriority(orderValue, createTime, valueWeight=0.3, timeWeight=0.7):
    return valueWeight*orderValue/50-timeWeight*createTime

''' print(orderId) '''
def printByOrder(orderId):
    global orders, nodes
    orderString = f"{orderId}"
    orderString += f", {nodes[orderId].createTime}"
    orderString += f", {nodes[orderId].value}"
    orderString += f", {nodes[orderId].deliveryTime}"
    orderString += f", {nodes[orderId].eta}"
    return "["+orderString+"]\n"

''' print(time1, time2) '''
def printByTime(time1, time2):
    global orders
    orderString = ""
    for i in range(currentOrderSize):
        if orders["eta"][i] > time2:
            break
        elif orders["eta"][i] >= time1:
            orderString += f"{orders['ID'][i]}, "
    if len(orderString) > 0:
        return "["+orderString[:-2]+"]\n"
    else:
        return "There are no orders in that time period\n"

''' getRankOfOrder(orderId) '''
def getRankOfOrder(orderId):
    global orders
    if orderId not in orders["ID"]:
        return ""
    num = orders["ID"].index(orderId)

    return f"Order {orderId} will be delivered after {num} orders.\n"

''' createOrder(orderId, currentSystemTime, orderValue, deliveryTime) '''
def createOrder(orderId, currentSystemTime, orderValue, deliveryTime):
    global orders, nodes, currentOrderSize, lastReturnTime
    ''' Check all delivered orders (and possibly one out for delivery) '''
    timestamp = max(currentSystemTime, lastReturnTime)
    startIdx = 0    # from which order we need to consider
    deliveredOrders = {}
    for i in range(currentOrderSize):
        if currentSystemTime >= orders["eta"][i]:    # I thought it's >=
            # order has been delivered
            orders["deliveryStatus"][i] = 2
            deliveredOrders[orders["ID"][i]] = orders["eta"][i]
            startIdx = i+1

            # if delivery man is still on his way back
            timestamp = max(timestamp, orders["eta"][i]+orders["deliveryTime"][i])
            lastReturnTime = orders["eta"][i]+orders["deliveryTime"][i]

        elif currentSystemTime >= orders["eta"][i]-orders["deliveryTime"][i]:
            # order is out for delivery
            orders["deliveryStatus"][i] = 1
            startIdx = i+1
            timestamp = orders["eta"][i]+orders["deliveryTime"][i]
            lastReturnTime = orders["eta"][i]+orders["deliveryTime"][i]
            break

        else:
            # delivery man is idle
            break

    ''' Calculate priority '''
    priority = getPriority(orderValue, currentSystemTime)

    # search where to put in the order and get eta
    insertRank = currentOrderSize
    for i in range(startIdx, currentOrderSize):
        if priority > orders["priority"][i]:
            insertRank = i
            break
    
    if insertRank == startIdx:
        orderETA = timestamp + deliveryTime
    else:
        prevEndTime = orders["eta"][insertRank-1] + orders["deliveryTime"][insertRank-1]
        orderETA = prevEndTime + deliveryTime

    
    ''' Update other orders '''
    updatedOrders = {}
    prevEndTime = orderETA + deliveryTime
    for i in range(insertRank, currentOrderSize):
        currStartTime = orders["eta"][i]-orders["deliveryTime"][i]
        if currStartTime < prevEndTime:
            offset = abs(prevEndTime-currStartTime)
            orders["eta"][i] += offset
            updatedOrders[orders["ID"][i]] = orders["eta"][i]
        prevEndTime = orders["eta"][i]+orders["deliveryTime"][i]

    ''' Output '''
    outputStr = ""
    # created order
    outputStr += f"Order {orderId} has been created - ETA: {orderETA}\n"

    # updated orders
    if len(updatedOrders) > 0:
        updatedString = ""
        for id, eta in updatedOrders.items():
            updatedString += f"{id}: {eta}, "
        outputStr += "Updated ETAs: [" + updatedString[:-2] + "]\n"

    # delivered orders
    if len(deliveredOrders) > 0:
        for id, eta in deliveredOrders.items():
            outputStr += f"Order {id} has been delivered at time {eta}\n"

    ''' Update dictionary and data structure '''
    # insert to lists
    orders["ID"].insert(insertRank, orderId)
    orders["eta"].insert(insertRank, orderETA)
    orders["priority"].insert(insertRank, priority)
    orders["deliveryTime"].insert(insertRank, deliveryTime)
    orders["deliveryStatus"].insert(insertRank, 0)

    cutIdx = startIdx if startIdx == 0 or orders["deliveryStatus"][startIdx-1] == 2 else startIdx-1
    for i in range(cutIdx):
        removeKey = orders["priority"][i]
        removeId = orders["ID"][i]
        myTree.delete(myTree.root, removeKey, removeId)   # remove from tree
        nodes[orders["ID"][i]] = None           # destroy node
        del nodes[orders["ID"][i]]              # remove from dict
    
    for l in orders.values():
        del l[:cutIdx]                          # remove from lists

    # insert to tree and dict
    newOrderNode = treeNode(orderId, currentSystemTime, orderValue, deliveryTime, orderETA, priority)
    # myTree.inorderTraversal(myTree.root)
    myTree.insert(myTree.root, newOrderNode)
    nodes[orderId] = newOrderNode
    
    # update order size
    currentOrderSize = currentOrderSize - cutIdx + 1

    return outputStr

''' cancelOrder(orderId, currentSystemTime) '''
def cancelOrder(orderId, currentSystemTime):
    global orders, nodes, currentOrderSize, lastReturnTime
    outputStr = ""

    ''' Check delivered orders '''
    deliveredOrders = {}
    for i in range(currentOrderSize):
        if currentSystemTime >= orders["eta"][i]:
            deliveredOrders[orders["ID"][i]] = orders["eta"][i]
        else:
            break

    ''' Check if the order can be cancelled '''
    if orderId not in nodes or currentSystemTime >= nodes[orderId].eta - nodes[orderId].deliveryTime:
        outputStr += f"Cannot cancel. Order {orderId} has already been delivered.\n"
        
        # delivered orders
        if len(deliveredOrders) > 0:
            for id, eta in deliveredOrders.items():
                outputStr += f"Order {id} has been delivered at time {eta}\n"

            for l in orders.values():
                del l[:len(deliveredOrders)]    # delete delivered from dict

            for id in deliveredOrders:          # destroy delivered and remove from dict
                keyForDel = nodes[id].priority
                idForDel = nodes[id].id
                nodes[id] = None
                del nodes[id]
                myTree.delete(myTree.root, keyForDel, idForDel)

            currentOrderSize -= len(deliveredOrders)       # update order size
                
        return outputStr
    
    ''' Update other orders '''
    orderIdx = orders["ID"].index(orderId)
    if orderIdx == currentOrderSize - 1:
        # delete it directly
        for l in orders.values():
            del l[orderIdx]
        nodes[orderId] = None
        del nodes[orderId]
        currentOrderSize -= 1
        return f"Order {orderId} has been canceled\n"
    
    # updated orders should be based on timestamp
    timestamp = max(currentSystemTime, lastReturnTime)
    if orderIdx >= 1:
        timestamp = max(timestamp, orders["eta"][orderIdx-1]+orders["deliveryTime"][orderIdx-1])

    updatedOrders = {}
    prevEndTime = timestamp
    for i in range(orderIdx+1, currentOrderSize):
        currStartTime = orders["eta"][i] - orders["deliveryTime"][i]
        if currStartTime > prevEndTime:
            offset = abs(currStartTime-prevEndTime)
            orders["eta"][i] -= offset
            updatedOrders[orders["ID"][i]] = orders["eta"][i]
        prevEndTime = orders["eta"][i] + orders["deliveryTime"][i]

    ''' Output '''
    # order cancelled
    outputStr += f"Order {orderId} has been canceled\n"

    # updated orders
    if len(updatedOrders) > 0:
        updatedString = ""
        for id, eta in updatedOrders.items():
            updatedString += f"{id}: {eta}, "
        outputStr += "Updated ETAs: [" + updatedString[:-2] + "]\n"

    # delivered orders
    if len(deliveredOrders) > 0:
        for id, eta in deliveredOrders.items():
            outputStr += f"Order {id} has been delivered at time {eta}\n"
    
    ''' Update dictionary and data structure '''
    for l in orders.values():
        del l[orderIdx]                 # delete canceled from dict
        del l[:len(deliveredOrders)]    # delete delivered from dict
    
    keyForDel = nodes[orderId].priority
    idForDel = nodes[orderId].id
    nodes[orderId] = None               # destroy object
    del nodes[orderId]                  # remove from dict
    myTree.delete(myTree.root, keyForDel, idForDel)

    for id in deliveredOrders:          # destroy delivered and remove from dict
        keyForDel = nodes[id].priority
        idForDel = nodes[id].id
        nodes[id] = None
        del nodes[id]
        myTree.delete(myTree.root, keyForDel, idForDel)

    currentOrderSize -= (1 + len(deliveredOrders))       # update order size

    return outputStr

''' updateTime(orderId, currentSystemTime, newDeliveryTime) '''
def updateTime(orderId, currentSystemTime, newDeliveryTime):
    global orders, nodes, currentOrderSize
    outputStr = ""

    ''' Check delivered orders '''
    deliveredOrders = {}
    for i in range(currentOrderSize):
        if currentSystemTime >= orders["eta"][i]:
            deliveredOrders[orders["ID"][i]] = orders["eta"][i]
        else:
            break

    # delete delivered orders from dict
    for l in orders.values():
        del l[:len(deliveredOrders)]
    
    currentOrderSize -= len(deliveredOrders)

    # delete from nodes
    for id in deliveredOrders:
        keyForDel = nodes[id].priority
        idForDel = nodes[id].id
        nodes[id] = None
        del nodes[id]
        myTree.delete(myTree.root, keyForDel, idForDel)
    
    ''' Check if the order can be updated '''
    if orderId not in nodes or currentSystemTime >= nodes[orderId].eta - nodes[orderId].deliveryTime:
        outputStr += f"Cannot update. Order {orderId} has already been delivered.\n"
        
        # delivered orders
        if len(deliveredOrders) > 0:
            for id, eta in deliveredOrders.items():
                outputStr += f"Order {id} has been delivered at time {eta}\n"
        
        return outputStr
    
    
    ''' Updated orders '''
    orderIdx = orders["ID"].index(orderId)
    offset = newDeliveryTime - orders["deliveryTime"][orderIdx]
    orders["eta"][orderIdx] += offset                   # update eta
    orders["deliveryTime"][orderIdx] = newDeliveryTime  # update delivery time
    nodes[orderId].eta = orders["eta"][orderIdx]        # update nodes
    nodes[orderId].deliveryTime = newDeliveryTime

    updatedOrders = {}
    if offset != 0:
        updatedOrders[orderId] = orders["eta"][orderIdx]

    prevEndTime = orders["eta"][orderIdx] + newDeliveryTime
    for i in range(orderIdx+1, currentOrderSize):
        currStartTime = orders["eta"][i] - orders["deliveryTime"][i]
        offset = prevEndTime - currStartTime
        orders["eta"][i] += offset              # update dict
        nodes[orders["ID"][i]].eta += offset    # update nodes
        updatedOrders[orders["ID"][i]] = orders["eta"][i]
        prevEndTime = orders["eta"][i] + orders["deliveryTime"][i]


    ''' Output '''
    # updated orders
    if len(updatedOrders) > 0:
        updatedString = ""
        for id, eta in updatedOrders.items():
            updatedString += f"{id}: {eta}, "
        outputStr += "Updated ETAs: [" + updatedString[:-2] + "]\n"
    
    # delivered orders
    if len(deliveredOrders) > 0:
        for id, eta in deliveredOrders.items():
            outputStr += f"Order {id} has been delivered at time {eta}\n"
    
    return outputStr

''' Output the remaining orders when Quit() '''
def outputRemaining():
    outputStr = ""
    for id, eta in zip(orders["ID"], orders["eta"]):
        outputStr += f"Order {id} has been delivered at time {eta}\n"

    return outputStr

def processCommand(cmd):
    cmd = cmd.strip().split('(')
    cmdType = cmd[0]
    argStr = cmd[1][:-1]
    args = argStr.split(",")
    args = [arg.strip() for arg in args]
    
    if cmdType == "print" and len(args) == 1:
        orderId = int(args[0])
        return printByOrder(orderId)
    
    elif cmdType == "print" and len(args) == 2:
        time1, time2 = map(int, args)
        return printByTime(time1, time2)
    
    elif cmdType == "getRankOfOrder" and len(args) == 1:
        orderId = int(args[0])
        return getRankOfOrder(orderId)
    
    elif cmdType == "createOrder" and len(args) == 4:
        orderId, currentSystemTime, orderValue, deliveryTime = map(int, args)
        return createOrder(orderId, currentSystemTime, orderValue, deliveryTime)
    
    elif cmdType == "cancelOrder" and len(args) == 2:
        orderId, currentSystemTime = map(int, args)
        return cancelOrder(orderId, currentSystemTime)
    
    elif cmdType == "updateTime" and len(args) == 3:
        orderId, currentSystemTime, newDeliveryTime = map(int, args)
        return updateTime(orderId, currentSystemTime, newDeliveryTime)

    else:
        print("Please enter a valid command.")
        return ""

if __name__ == "__main__":
    import sys

    inputFile = sys.argv[1]
    fileName = inputFile[:-4]

    outputStr = ""

    with open(inputFile, 'r') as f:
        cmd = f.readline()
        while not cmd.startswith("Quit()"):
            outputStr += processCommand(cmd[:-1])
            cmd = f.readline()

    if cmd.startswith("Quit()"):
        # print out all remaining orders
        outputStr += outputRemaining()

    with open(f"{fileName}_output_file.txt", 'w') as f:
        f.writelines(outputStr)
